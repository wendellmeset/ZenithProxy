package com.zenith;

import ch.qos.logback.classic.LoggerContext;
import com.zenith.cache.CacheResetType;
import com.zenith.discord.Embed;
import com.zenith.event.proxy.*;
import com.zenith.feature.api.crafthead.CraftheadApi;
import com.zenith.feature.api.mcsrvstatus.MCSrvStatusApi;
import com.zenith.feature.api.minotar.MinotarApi;
import com.zenith.feature.autoupdater.AutoUpdater;
import com.zenith.feature.autoupdater.NoOpAutoUpdater;
import com.zenith.feature.autoupdater.RestAutoUpdater;
import com.zenith.feature.queue.Queue;
import com.zenith.module.impl.AutoReconnect;
import com.zenith.network.client.Authenticator;
import com.zenith.network.client.ClientSession;
import com.zenith.network.server.CustomServerInfoBuilder;
import com.zenith.network.server.LanBroadcaster;
import com.zenith.network.server.ProxyServerListener;
import com.zenith.network.server.ServerSession;
import com.zenith.network.server.handler.ProxyServerLoginHandler;
import com.zenith.util.ComponentSerializer;
import com.zenith.util.FastArrayList;
import com.zenith.util.Wait;
import com.zenith.via.ZenithClientChannelInitializer;
import com.zenith.via.ZenithServerChannelInitializer;
import io.netty.util.ResourceLeakDetector;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import net.raphimc.minecraftauth.responsehandler.exception.MinecraftRequestException;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.network.BuiltinFlags;
import org.geysermc.mcprotocollib.network.ProxyInfo;
import org.geysermc.mcprotocollib.network.tcp.TcpConnectionManager;
import org.geysermc.mcprotocollib.network.tcp.TcpServer;
import org.geysermc.mcprotocollib.protocol.MinecraftConstants;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodecHelper;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.BuiltinSound;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.SoundCategory;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundTabListPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSoundPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.title.ClientboundSetActionBarTextPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundSetCarriedItemPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.*;
import static com.zenith.util.Config.Authentication.AccountType.MSA;
import static com.zenith.util.Config.Authentication.AccountType.OFFLINE;
import static java.util.Objects.nonNull;


@Getter
public class Proxy {
    @Getter protected static final Proxy instance = new Proxy();
    protected ClientSession client;
    protected TcpServer server;
    protected final Authenticator authenticator = new Authenticator();
    protected byte[] serverIcon;
    protected final AtomicReference<ServerSession> currentPlayer = new AtomicReference<>();
    protected final FastArrayList<ServerSession> activeConnections = new FastArrayList<>(ServerSession.class);
    private boolean inQueue = false;
    private boolean didQueueSkip = false;
    private int queuePosition = 0;
    @Setter @Nullable private Instant connectTime;
    private Instant disconnectTime = Instant.now();
    private OptionalLong prevOnlineSeconds = OptionalLong.empty();
    private Optional<Boolean> isPrio = Optional.empty();
    @Getter private final AtomicBoolean loggingIn = new AtomicBoolean(false);
    @Setter @NotNull private AutoUpdater autoUpdater = NoOpAutoUpdater.INSTANCE;
    private LanBroadcaster lanBroadcaster;
    // might move to config and make the user deal with it when it changes
    private static final Duration twoB2tTimeLimit = Duration.ofHours(6);
    private TcpConnectionManager tcpManager;

    public static void main(String... args) {
        Locale.setDefault(Locale.ENGLISH);
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        if (System.getProperty("io.netty.leakDetection.level") == null)
            ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
        if (System.getProperty("reactor.schedulers.defaultPoolSize") == null)
            System.setProperty("reactor.schedulers.defaultPoolSize", "1");
        if (System.getProperty("reactor.schedulers.defaultBoundedElasticOnVirtualThreads") == null)
            System.setProperty("reactor.schedulers.defaultBoundedElasticOnVirtualThreads", "true");
        instance.start();
    }

    public void initEventHandlers() {
        EVENT_BUS.subscribe(
            this,
            of(DisconnectEvent.class, this::handleDisconnectEvent),
            of(ConnectEvent.class, this::handleConnectEvent),
            of(StartQueueEvent.class, this::handleStartQueueEvent),
            of(QueuePositionUpdateEvent.class, this::handleQueuePositionUpdateEvent),
            of(QueueCompleteEvent.class, this::handleQueueCompleteEvent),
            of(QueueSkipEvent.class, this::handleQueueSkipEvent),
            of(PlayerOnlineEvent.class, this::handlePlayerOnlineEvent),
            of(PrioStatusEvent.class, this::handlePrioStatusEvent),
            of(PrivateMessageSendEvent.class, this::handlePrivateMessageSendEvent)
        );
    }

    public void start() {
        DEFAULT_LOG.info("Starting ZenithProxy-{}", LAUNCH_CONFIG.version);
        @Nullable String exeReleaseVersion = getExecutableReleaseVersion();
        if (exeReleaseVersion == null) {
            DEFAULT_LOG.warn("Detected unofficial ZenithProxy development build!");
        } else if (!LAUNCH_CONFIG.version.split("\\+")[0].equals(exeReleaseVersion.split("\\+")[0])) {
            DEFAULT_LOG.warn("launch_config.json version: {} and embedded ZenithProxy version: {} do not match!", LAUNCH_CONFIG.version, exeReleaseVersion);
            if (LAUNCH_CONFIG.auto_update)
                DEFAULT_LOG.warn("AutoUpdater is enabled but will break!");
            DEFAULT_LOG.warn("Use the official launcher: https://github.com/rfresh2/ZenithProxy/releases/tag/launcher-v3");
        }
        initEventHandlers();
        try {
            if (CONFIG.debug.clearOldLogs) EXECUTOR.schedule(Proxy::clearOldLogs, 10L, TimeUnit.SECONDS);
            if (CONFIG.interactiveTerminal.enable) TERMINAL.start();
            MODULE.init();
            this.tcpManager = new TcpConnectionManager();
            if (CONFIG.database.enabled) {
                DATABASE.start();
                DEFAULT_LOG.info("Started Databases");
            }
            if (CONFIG.discord.enable) {
                boolean err = false;
                try {
                    DISCORD.start();
                } catch (final Throwable e) {
                    err = true;
                    DISCORD_LOG.error("Failed starting discord bot: {}", e.getMessage());
                    DISCORD_LOG.debug("Failed starting discord bot", e);
                }
                if (!err) DISCORD_LOG.info("Started Discord Bot");
            }
            Queue.start();
            saveConfigAsync();
            MinecraftCodecHelper.useBinaryNbtComponentSerializer = CONFIG.debug.binaryNbtComponentSerializer;
            MinecraftConstants.CHUNK_SECTION_COUNT_PROVIDER = CACHE.getSectionCountProvider();
            if (CONFIG.client.viaversion.enabled || CONFIG.server.viaversion.enabled) {
                VIA_INITIALIZER.init();
            }
            startServer();
            CACHE.reset(CacheResetType.FULL);
            EXECUTOR.scheduleAtFixedRate(this::serverHealthCheck, 1L, 5L, TimeUnit.MINUTES);
            EXECUTOR.scheduleAtFixedRate(this::tablistUpdate, 20L, 3L, TimeUnit.SECONDS);
            EXECUTOR.scheduleAtFixedRate(this::twoB2tTimeLimitKickWarningTick, twoB2tTimeLimit.minusMinutes(10L).toMinutes(), 1L, TimeUnit.MINUTES);
            EXECUTOR.scheduleAtFixedRate(this::maxPlaytimeTick, CONFIG.client.maxPlaytimeReconnectMins, 1L, TimeUnit.MINUTES);
            EXECUTOR.schedule(this::serverConnectionTest, 10L, TimeUnit.SECONDS);
            if (CONFIG.server.enabled && CONFIG.server.ping.favicon)
                EXECUTOR.submit(this::updateFavicon);
            boolean connected = false;
            if (CONFIG.client.autoConnect && !isConnected()) {
                connectAndCatchExceptions();
                connected = true;
            }
            if (!connected && CONFIG.autoUpdater.shouldReconnectAfterAutoUpdate) {
                CONFIG.autoUpdater.shouldReconnectAfterAutoUpdate = false;
                saveConfigAsync();
                if (!CONFIG.client.extra.utility.actions.autoDisconnect.autoClientDisconnect && !isConnected()) {
                    connectAndCatchExceptions();
                    connected = true;
                }
            }
            if (LAUNCH_CONFIG.auto_update) {
                autoUpdater = LAUNCH_CONFIG.release_channel.equals("git")
                    ? NoOpAutoUpdater.INSTANCE
                    : new RestAutoUpdater();
                autoUpdater.start();
                DEFAULT_LOG.info("Started AutoUpdater");
            }
            DEFAULT_LOG.info("ZenithProxy started!");
            if (!DISCORD.isRunning() && LAUNCH_CONFIG.release_channel.endsWith(".pre")) {
                DEFAULT_LOG.warn("You are currently using a ZenithProxy prerelease");
                DEFAULT_LOG.warn("Prereleases include experiments that may contain bugs and are not always updated with fixes");
                DEFAULT_LOG.warn("Switch to a stable release with the `channel` command");
            }
            if (!connected) {
                DEFAULT_LOG.info("Commands Help: https://github.com/rfresh2/ZenithProxy/wiki/Commands");
                DEFAULT_LOG.info("Proxy IP: {}", CONFIG.server.getProxyAddress());
                DEFAULT_LOG.info("Use the `connect` command to log in!");
            }
            Wait.waitSpinLoop();
        } catch (Exception e) {
            DEFAULT_LOG.error("", e);
        } finally {
            DEFAULT_LOG.info("Shutting down...");
            if (this.server != null) this.server.close(true);
            saveConfig();
        }
    }

    private static void clearOldLogs() {
        try (Stream<Path> walk = Files.walk(Path.of("log/"))) {
            walk.filter(path -> path.toString().endsWith(".zip")).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (final IOException e) {
                    DEFAULT_LOG.error("Error deleting old log file", e);
                }
            });
        } catch (final IOException e) {
            DEFAULT_LOG.error("Error deleting old log file", e);
        }
    }

    private void serverHealthCheck() {
        if (!CONFIG.server.enabled || !CONFIG.server.healthCheck) return;
        if (server != null && server.isListening()) return;
        SERVER_LOG.error("Server is not listening! Is another service on this port?");
        this.startServer();
        EXECUTOR.schedule(() -> {
            if (server == null || !server.isListening()) {
                var errorMessage = """
                    The ZenithProxy MC server was unable to start correctly.
                    
                    Most likely you have two or more ZenithProxy instance running on the same configured port: %s.
                    
                    Shut down duplicate instances, or change the configured port: `serverConnection port <port>`
                    """.formatted(CONFIG.server.bind.port);
                SERVER_LOG.error(errorMessage);
                if (DISCORD.isRunning()) {
                    DISCORD.sendEmbedMessage(
                        Embed.builder()
                            .title("ZenithProxy Server Error")
                            .description(errorMessage)
                            .errorColor());
                }
            }
        }, 30, TimeUnit.SECONDS);
    }

    private void serverConnectionTest() {
        if (!CONFIG.server.connectionTestOnStart) return;
        if (!CONFIG.server.enabled) return;
        if (server == null || !server.isListening()) return;
        if (!CONFIG.server.ping.enabled) return;
        var address = CONFIG.server.getProxyAddress();
        if (address.startsWith("localhost")) {
            SERVER_LOG.debug("Proxy IP is set to localhost, skipping connection test");
            return;
        }
        MCSrvStatusApi.INSTANCE.getMCSrvStatus(CONFIG.server.getProxyAddress())
            .ifPresentOrElse(response -> {
                if (response.online()) {
                    SERVER_LOG.debug("Connection test successful: {}", address);
                } else {
                    SERVER_LOG.error(
                        """
                        Unable to ping the configured `proxyIP`: {}
                        
                        If you are actually able to connect to ZenithProxy you can disable this test: `connectionTest testOnStart off`
                        
                        This test is most likely failing due to a firewall needing to be disabled.
                        
                        If the `proxyIP` is incorrect, set `serverConnection proxyIP <ip>` with the correct IP.
                        
                        For instructions on how to disable the firewall consult with your VPS provider. Each provider varies in steps and what word they refer to firewalls with.
                        """, address);
                }
            }, () -> {
                SERVER_LOG.debug("Failed trying to perform connection test");
                // reschedule another attempt?
            });
    }

    private void maxPlaytimeTick() {
        if (CONFIG.client.maxPlaytimeReconnect && isOnlineForAtLeastDuration(Duration.ofMinutes(CONFIG.client.maxPlaytimeReconnectMins))) {
            CLIENT_LOG.info("Max playtime minutes reached: {}, reconnecting...", CONFIG.client.maxPlaytimeReconnectMins);
            disconnect(MAX_PT_DISCONNECT);
            MODULE.get(AutoReconnect.class).cancelAutoReconnect();
            connect();
        }
    }

    private void tablistUpdate() {
        var playerConnection = currentPlayer.get();
        if (!this.isConnected() || playerConnection == null) return;
        if (!playerConnection.isLoggedIn()) return;
        long lastUpdate = CACHE.getTabListCache().getLastUpdate();
        if (lastUpdate < System.currentTimeMillis() - 3000) {
            playerConnection.sendAsync(new ClientboundTabListPacket(CACHE.getTabListCache().getHeader(), CACHE.getTabListCache().getFooter()));
            CACHE.getTabListCache().setLastUpdate(System.currentTimeMillis());
        }
    }

    public void stop() {
        DEFAULT_LOG.info("Shutting Down...");
        try {
            CompletableFuture.runAsync(() -> {
                if (nonNull(this.client)) this.client.disconnect(MinecraftConstants.SERVER_CLOSING_MESSAGE);
                MODULE.get(AutoReconnect.class).cancelAutoReconnect();
                stopServer();
                tcpManager.close();
                saveConfig();
                int count = 0;
                while (!DISCORD.isMessageQueueEmpty() && count++ < 10) {
                    Wait.waitMs(100);
                }
                DISCORD.stop(true);
            }).get(10L, TimeUnit.SECONDS);
        } catch (final Exception e) {
            DEFAULT_LOG.error("Error shutting down gracefully", e);
        } finally {
            try {
                ((LoggerContext) LoggerFactory.getILoggerFactory()).stop();
            } finally {
                System.exit(0);
            }
        }
    }

    public void disconnect() {
        disconnect(MANUAL_DISCONNECT);
    }

    public void disconnect(final String reason, final Throwable cause) {
        if (this.isConnected()) {
            if (CONFIG.debug.kickDisconnect) this.kickDisconnect(reason, cause);
            else this.client.disconnect(reason, cause);
        }
    }

    public void disconnect(final String reason) {
        if (this.isConnected()) {
            if (CONFIG.debug.kickDisconnect) this.kickDisconnect(reason, null);
            else this.client.disconnect(reason);
        }
    }

    public void kickDisconnect(final String reason, final Throwable cause) {
        if (!isConnected()) return;
        var client = this.client;

        try {
            // must send direct to avoid any caching issues from outbound handlers
            client.send(new ServerboundSetCarriedItemPacket(10)).get();
        } catch (final Exception e) {
            CLIENT_LOG.error("Error performing kick disconnect", e);
        }
        // note: this will occur before the server sends us back a disconnect packet, but before our channel close is received by the server
        client.disconnect(reason, cause);
    }

    public void connectAndCatchExceptions(String serverIP, int serverPort) {
        try {
            this.connect(serverIP, serverPort);  // Use the provided arguments here
        } catch (final Exception e) {
            DEFAULT_LOG.error("Error connecting to {}:{}", serverIP, serverPort, e);
        }
    }

    /**
     * @throws IllegalStateException if already connected
     */
    public synchronized void connect(String serverIP, int serverPort) {
        connect(serverIP, serverPort);  // Call the method with the provided serverIP and serverPort
    }

    public synchronized void connect(final String address, final int port) {
        if (this.isConnected()) throw new IllegalStateException("Already connected!");
        if (this.client != null && !this.client.isDisconnected()) throw new IllegalStateException("Not Disconnected!");
        this.connectTime = Instant.now();
        final MinecraftProtocol minecraftProtocol;
        try {
            EVENT_BUS.postAsync(new StartConnectEvent());
            minecraftProtocol = this.logIn();
        } catch (final Exception e) {
            EVENT_BUS.post(new ProxyLoginFailedEvent());
            var connections = getActiveConnections().getArray();
            for (int i = 0; i < connections.length; i++) {
                var connection = connections[i];
                connection.disconnect("Login failed");
            }
            EXECUTOR.schedule(() -> EVENT_BUS.post(new DisconnectEvent(LOGIN_FAILED)), 1L, TimeUnit.SECONDS);
            return;
        }
        CLIENT_LOG.info("Connecting to {}:{}...", address, port);
        this.client = new ClientSession(address, port, CONFIG.client.bindAddress, minecraftProtocol, getClientProxyInfo(), tcpManager);
        if (Objects.equals(address, "connect.2b2t.org"))
            this.client.setFlag(BuiltinFlags.ATTEMPT_SRV_RESOLVE, false);
        this.client.setReadTimeout(CONFIG.client.timeout.enable ? CONFIG.client.timeout.seconds : 0);
        this.client.setFlag(MinecraftConstants.CLIENT_CHANNEL_INITIALIZER, ZenithClientChannelInitializer.FACTORY);
        this.client.connect(true);
        // wait for connection state to stabilize
        Wait.waitUntil(() -> this.client.isConnected() || this.client.isDisconnected(), 30);
    }

    @Nullable
    private static ProxyInfo getClientProxyInfo() {
        ProxyInfo proxyInfo = null;
        if (CONFIG.client.connectionProxy.enabled) {
            if (!CONFIG.client.connectionProxy.user.isEmpty() || !CONFIG.client.connectionProxy.password.isEmpty())
                proxyInfo = new ProxyInfo(CONFIG.client.connectionProxy.type,
                                          new InetSocketAddress(CONFIG.client.connectionProxy.host,
                                                                CONFIG.client.connectionProxy.port),
                                          CONFIG.client.connectionProxy.user,
                                          CONFIG.client.connectionProxy.password);
            else proxyInfo = new ProxyInfo(CONFIG.client.connectionProxy.type,
                                           new InetSocketAddress(CONFIG.client.connectionProxy.host,
                                                                 CONFIG.client.connectionProxy.port));
        }
        return proxyInfo;
    }

    public boolean isConnected() {
        return this.client != null && this.client.isConnected();
    }

    @SneakyThrows
    public synchronized void startServer() {
        if (this.server != null && this.server.isListening())
            throw new IllegalStateException("Server already started!");
        if (!CONFIG.server.enabled) return;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("servericon.png")) {
            this.serverIcon = in.readAllBytes();
        }
        var address = CONFIG.server.bind.address;
        var port = CONFIG.server.bind.port;
        SERVER_LOG.info("Starting server on {}:{}...", address, port);
        this.server = new TcpServer(address, port, MinecraftProtocol::new, tcpManager, (socketAddress) -> new ServerSession(socketAddress.getHostName(), socketAddress.getPort(), (MinecraftProtocol) server.createPacketProtocol(), server));
        this.server.setGlobalFlag(MinecraftConstants.SERVER_CHANNEL_INITIALIZER, ZenithServerChannelInitializer.FACTORY);
        var serverInfoBuilder = new CustomServerInfoBuilder();
        this.server.setGlobalFlag(MinecraftConstants.SERVER_INFO_BUILDER_KEY, serverInfoBuilder);
        if (this.lanBroadcaster == null && CONFIG.server.ping.lanBroadcast) {
            this.lanBroadcaster = new LanBroadcaster(serverInfoBuilder);
            lanBroadcaster.start();
        }
        this.server.setGlobalFlag(MinecraftConstants.SERVER_LOGIN_HANDLER_KEY, new ProxyServerLoginHandler());
        this.server.setGlobalFlag(MinecraftConstants.AUTOMATIC_KEEP_ALIVE_MANAGEMENT, true);
        this.server.addListener(new ProxyServerListener());
        this.server.bind(false);
    }

    public synchronized void stopServer() {
        SERVER_LOG.info("Stopping server...");
        if (this.server != null && this.server.isListening()) this.server.close(true);
        if (this.lanBroadcaster != null) {
            this.lanBroadcaster.stop();
            this.lanBroadcaster = null;
        }
    }

    public synchronized @NonNull MinecraftProtocol logIn() {
        if (!loggingIn.compareAndSet(false, true)) throw new RuntimeException("Already logging in!");
        AUTH_LOG.info("Logging in {}...", CONFIG.authentication.username);
        MinecraftProtocol minecraftProtocol = null;
        for (int tries = 0; tries < 3; tries++) {
            minecraftProtocol = retrieveLoginTaskResult(loginTask());
            if (minecraftProtocol != null || !loggingIn.get()) break;
            AUTH_LOG.warn("Failed login attempt " + (tries + 1));
            Wait.wait((int) (3 + (Math.random() * 7.0)));
        }
        if (!loggingIn.compareAndSet(true, false)) throw new RuntimeException("Login Cancelled");
        if (minecraftProtocol == null) throw new RuntimeException("Auth failed");
        var username = minecraftProtocol.getProfile().getName();
        var uuid = minecraftProtocol.getProfile().getId();
        CACHE.getChatCache().setPlayerCertificates(minecraftProtocol.getProfile().getPlayerCertificates());
        AUTH_LOG.info("Logged in as {} [{}].", username, uuid);
        if (CONFIG.server.extra.whitelist.autoAddClient && CONFIG.authentication.accountType != OFFLINE)
            if (PLAYER_LISTS.getWhitelist().add(username, uuid))
                SERVER_LOG.info("Auto added {} [{}] to whitelist", username, uuid);
        EXECUTOR.execute(this::updateFavicon);
        return minecraftProtocol;
    }

    public Future<MinecraftProtocol> loginTask() {
        return EXECUTOR.submit(() -> {
            try {
                return this.authenticator.login();
            } catch (final Exception e) {
                if (e instanceof InterruptedException) {
                    return null;
                }
                CLIENT_LOG.error("Login failed", e);
                if (e instanceof MinecraftRequestException mre) {
                    if (mre.getResponse().getStatusCode() == 404) {
                        AUTH_LOG.error("[Help] Log into the account with the vanilla MC launcher and join a server. Then try again with ZenithProxy.");
                    }
                }
                return null;
            }
        });
    }

    public MinecraftProtocol retrieveLoginTaskResult(Future<MinecraftProtocol> loginTask) {
        try {
            var maxWait = CONFIG.authentication.accountType == MSA ? 10 : 300;
            for (int currentWait = 0; currentWait < maxWait; currentWait++) {
                if (loginTask.isDone()) break;
                if (!loggingIn.get()) {
                    loginTask.cancel(true);
                    return null;
                }
                Wait.wait(1);
            }
            return loginTask.get(1L, TimeUnit.SECONDS);
        } catch (Exception e) {
            loginTask.cancel(true);
            return null;
        }
    }

    public URL getAvatarURL(UUID uuid) {
        return getAvatarURL(uuid.toString().replace("-", ""));
    }

    public URL getAvatarURL(String playerName) {
        try {
            return URI.create(String.format("https://minotar.net/helm/%s/64", playerName)).toURL();
        } catch (MalformedURLException e) {
            SERVER_LOG.error("Failed to get avatar URL for player: {}", playerName, e);
            throw new UncheckedIOException(e);
        }
    }

    // returns true if we were previously trying to log in
    public boolean cancelLogin() {
        return this.loggingIn.getAndSet(false);
    }

    public List<ServerSession> getSpectatorConnections() {
        var connections = getActiveConnections().getArray();
        // optimize most frequent cases as fast-paths to avoid list alloc
        if (connections.length == 0) return Collections.emptyList();
        if (connections.length == 1 && hasActivePlayer()) return Collections.emptyList();
        final List<ServerSession> result = new ArrayList<>(hasActivePlayer() ? connections.length - 1 : connections.length);
        for (int i = 0; i < connections.length; i++) {
            var connection = connections[i];
            if (connection.isSpectator()) {
                result.add(connection);
            }
        }
        return result;
    }

    public boolean hasActivePlayer() {
        ServerSession player = this.currentPlayer.get();
        return player != null && player.isLoggedIn();
    }

    public @Nullable ServerSession getActivePlayer() {
        ServerSession player = this.currentPlayer.get();
        if (player != null && player.isLoggedIn()) return player;
        else return null;
    }

    public boolean isPrio() {
        return this.isPrio.orElse(CONFIG.authentication.prio);
    }

    public void kickNonWhitelistedPlayers() {
        var connections = Proxy.getInstance().getActiveConnections().getArray();
        for (int i = 0; i < connections.length; i++) {
            var connection = connections[i];
            if (connection.getProfileCache().getProfile() == null) continue;
            if (PLAYER_LISTS.getWhitelist().contains(connection.getProfileCache().getProfile())) continue;
            if (PLAYER_LISTS.getSpectatorWhitelist().contains(connection.getProfileCache().getProfile()) && connection.isSpectator()) continue;
            connection.disconnect("Not whitelisted");
        }
    }

    public boolean isOnlineOn2b2tForAtLeastDuration(Duration duration) {
        return isOn2b2t() && isOnlineForAtLeastDuration(duration);
    }

    public boolean isOnlineForAtLeastDuration(Duration duration) {
        return isConnected()
            && !isInQueue()
            && nonNull(getConnectTime())
            && getConnectTime().isBefore(Instant.now().minus(duration));
    }

    public void updateFavicon() {
        if (!CONFIG.authentication.username.equals("Unknown")) { // else use default icon
            try {
                final GameProfile profile = CACHE.getProfileCache().getProfile();
                if (profile != null && profile.getId() != null) {
                    // do uuid lookup
                    final UUID uuid = profile.getId();
                    this.serverIcon = MinotarApi.INSTANCE.getAvatar(uuid).or(() -> CraftheadApi.INSTANCE.getAvatar(uuid))
                        .orElseThrow(() -> new IOException("Unable to download server icon for \"" + uuid + "\""));
                } else {
                    // do username lookup
                    final String username = CONFIG.authentication.username;
                    this.serverIcon = MinotarApi.INSTANCE.getAvatar(username).or(() -> CraftheadApi.INSTANCE.getAvatar(username))
                        .orElseThrow(() -> new IOException("Unable to download server icon for \"" + username + "\""));
                }
                if (DISCORD.isRunning()) {
                    if (CONFIG.discord.manageNickname)
                        DISCORD.setBotNickname(CONFIG.authentication.username + " | ZenithProxy");
                    if (CONFIG.discord.manageDescription) DISCORD.setBotDescription(
                        """
                        ZenithProxy %s
                        **Official Discord**:
                          https://discord.gg/nJZrSaRKtb
                        **Github**:
                          https://github.com/rfresh2/ZenithProxy
                        """.formatted(LAUNCH_CONFIG.version));
                }
            } catch (final Throwable e) {
                SERVER_LOG.error("Failed updating favicon");
                SERVER_LOG.debug("Failed updating favicon", e);
            }
        }
        if (DISCORD.isRunning() && this.serverIcon != null)
            if (CONFIG.discord.manageProfileImage) DISCORD.updateProfileImage(this.serverIcon);
    }

    public void twoB2tTimeLimitKickWarningTick() {
        try {
            if (this.isPrio() // Prio players don't get kicked
                || !this.hasActivePlayer() // If no player is connected, nobody to warn
                || !isOnlineOn2b2tForAtLeastDuration(twoB2tTimeLimit.minusMinutes(10L))
            ) return;
            final ServerSession playerConnection = this.currentPlayer.get();
            final Duration durationUntilKick = twoB2tTimeLimit.minus(Duration.ofSeconds(Proxy.getInstance().getOnlineTimeSecondsWithQueueSkip()));
            if (durationUntilKick.isNegative()) return; // sanity check just in case 2b's plugin changes
            var actionBarPacket = new ClientboundSetActionBarTextPacket(
                ComponentSerializer.minimessage((durationUntilKick.toMinutes() <= 3 ? "<red>" : "<blue>") + twoB2tTimeLimit.toHours() + "hr kick in: " + durationUntilKick.toMinutes() + "m"));
            playerConnection.sendAsync(actionBarPacket);
            // each packet will reset text render timer for 3 seconds
            for (int i = 1; i <= 7; i++) { // render the text for about 10 seconds total
                playerConnection.sendScheduledAsync(actionBarPacket, i, TimeUnit.SECONDS);
            }
            playerConnection.sendAsync(new ClientboundSoundPacket(
                BuiltinSound.BLOCK_ANVIL_PLACE,
                SoundCategory.AMBIENT,
                CACHE.getPlayerCache().getX(),
                CACHE.getPlayerCache().getY(),
                CACHE.getPlayerCache().getZ(),
                1.0f,
                1.0f + (ThreadLocalRandom.current().nextFloat() / 10f), // slight pitch variations
                0L
            ));
        } catch (final Throwable e) {
            DEFAULT_LOG.error("Error in 2b2t time limit kick warning tick", e);
        }
    }

    public boolean isOn2b2t() {
        return CONFIG.client.server.address.toLowerCase().endsWith("2b2t.org");
    }

    public long getOnlineTimeSeconds() {
        var proxyConnectTime = this.connectTime;
        return proxyConnectTime != null
            ? TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) - proxyConnectTime.getEpochSecond()
            : 0L;
    }

    public long getOnlineTimeSecondsWithQueueSkip() {
        return !inQueue && didQueueSkip && prevOnlineSeconds.isPresent()
            ? getOnlineTimeSeconds() + prevOnlineSeconds.getAsLong()
            : getOnlineTimeSeconds();
    }

    public String getOnlineTimeString() {
        return Queue.getEtaStringFromSeconds(getOnlineTimeSecondsWithQueueSkip());
    }

    public void handleDisconnectEvent(DisconnectEvent event) {
        CACHE.reset(CacheResetType.FULL);
        this.disconnectTime = Instant.now();
        this.prevOnlineSeconds = inQueue
            ? OptionalLong.empty()
            : OptionalLong.of(Duration.between(this.connectTime, this.disconnectTime).toSeconds());
        this.inQueue = false;
        this.didQueueSkip = false;
        this.queuePosition = 0;
        TPS.reset();
        if (!DISCORD.isRunning()
            && isOn2b2t()
            && !isPrio()
            && event.reason().startsWith("You have lost connection")) {
            if (event.onlineDuration().toSeconds() >= 0L
                && event.onlineDuration().toSeconds() <= 1L) {
                CLIENT_LOG.warn("""
                                You have likely been kicked for reaching the 2b2t non-prio account IP limit.
                                Consider configuring a connection proxy with the `clientConnection` command.
                                Or migrate ZenithProxy instances to multiple hosts/IP's.
                                """);
            } else if (event.wasInQueue() && event.queuePosition() <= 1) {
                CLIENT_LOG.warn("""
                                You have likely been kicked due to being IP banned by 2b2t.
                                
                                To check, try connecting and waiting through queue with the same account from a different IP.
                                """);
            }
        }
    }

    public void handleConnectEvent(ConnectEvent event) {
        this.connectTime = Instant.now();
        if (isOn2b2t()) EXECUTOR.execute(Queue::updateQueueStatusNow);
    }

    public void handleStartQueueEvent(StartQueueEvent event) {
        this.inQueue = true;
        this.queuePosition = 0;
        if (event.wasOnline()) this.connectTime = Instant.now();
    }

    public void handleQueuePositionUpdateEvent(QueuePositionUpdateEvent event) {
        this.queuePosition = event.position();
    }

    public void handleQueueCompleteEvent(QueueCompleteEvent event) {
        this.inQueue = false;
        this.connectTime = Instant.now();
    }

    public void handleQueueSkipEvent(QueueSkipEvent event) {
        this.didQueueSkip = true;
    }

    public void handlePlayerOnlineEvent(PlayerOnlineEvent event) {
        if (this.isPrio.isEmpty())
            // assume we are prio if we skipped queuing
            EVENT_BUS.postAsync(new PrioStatusEvent(true));
    }

    public void handlePrioStatusEvent(PrioStatusEvent event) {
        if (!isOn2b2t()) return;
        if (event.prio() == CONFIG.authentication.prio) {
            if (isPrio.isEmpty()) {
                CLIENT_LOG.info("Prio Detected: {}", event.prio());
                this.isPrio = Optional.of(event.prio());
            }
        } else {
            CLIENT_LOG.info("Prio Change Detected: {}", event.prio());
            EVENT_BUS.postAsync(new PrioStatusUpdateEvent(event.prio()));
            this.isPrio = Optional.of(event.prio());
            CONFIG.authentication.prio = event.prio();
            saveConfigAsync();
        }
    }

    public void handlePrivateMessageSendEvent(PrivateMessageSendEvent event) {
        if (!isConnected()) return;
        CHAT_LOG.info("{}", ComponentSerializer.serializeJson(event.getContents()));
        var connections = getActiveConnections().getArray();
        for (int i = 0; i < connections.length; i++) {
            var connection = connections[i];
            connection.sendAsync(new ClientboundSystemChatPacket(event.getContents(), false));
        }
    }
}
