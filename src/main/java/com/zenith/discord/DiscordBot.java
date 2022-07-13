package com.zenith.discord;

import com.collarmc.pounce.Subscribe;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.zenith.Proxy;
import com.zenith.discord.command.*;
import com.zenith.event.proxy.*;
import com.zenith.util.Queue;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.object.presence.Status;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.MessageCreateRequest;
import discord4j.rest.RestClient;
import discord4j.rest.entity.RestChannel;
import discord4j.rest.util.Color;
import discord4j.rest.util.MultipartRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.zenith.discord.command.StatusCommand.getCoordinates;
import static com.zenith.util.Constants.*;
import static java.util.Objects.nonNull;

public class DiscordBot {

    private RestClient restClient;
    private Supplier<RestChannel> mainRestChannel = Suppliers.memoize(() -> restClient.getChannelById(Snowflake.of(CONFIG.discord.channelId)));
    private Supplier<RestChannel> relayRestChannel = Suppliers.memoize(() -> restClient.getChannelById(Snowflake.of(CONFIG.discord.chatRelay.channelId)));
    private GatewayDiscordClient client;
    private Proxy proxy;
    public List<Command> commands = new ArrayList<>();
    private static final ClientPresence DISCONNECTED_PRESENCE = ClientPresence.of(Status.DO_NOT_DISTURB, ClientActivity.playing("Disconnected"));
    private static final ClientPresence DEFAULT_CONNECTED_PRESENCE = ClientPresence.of(Status.ONLINE, ClientActivity.playing(CONFIG.client.server.address));
    private final ScheduledExecutorService presenceExecutorService;

    public DiscordBot() {
        this.presenceExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    public void start(Proxy proxy) {
        this.proxy = proxy;
        this.client = DiscordClient.create(CONFIG.discord.token)
                .gateway()
                .setInitialPresence(shardInfo -> DISCONNECTED_PRESENCE)
                .login()
                .block();
        EVENT_BUS.subscribe(this);

        restClient = client.getRestClient();

        commands.add(new ConnectCommand(this.proxy));
        commands.add(new DisconnectCommand(this.proxy));
        commands.add(new StatusCommand(this.proxy));
        commands.add(new HelpCommand(this.proxy, this.commands));
        commands.add(new WhitelistCommand(this.proxy));
        commands.add(new AutoDisconnectCommand(this.proxy));
        commands.add(new AutoReconnectCommand(this.proxy));
        commands.add(new AutoRespawnCommand(this.proxy));
        commands.add(new ServerCommand(this.proxy));
        commands.add(new AntiAFKCommand(this.proxy));
        commands.add(new VisualRangeCommand(this.proxy));
        commands.add(new UpdateCommand(this.proxy));
        commands.add(new ProxyClientConnectionCommand(this.proxy));
        commands.add(new ActiveHoursCommand(this.proxy));
        commands.add(new DisplayCoordsCommand(this.proxy));
        commands.add(new ChatRelayCommand(this.proxy));
        commands.add(new ReconnectCommand(this.proxy));

        client.getEventDispatcher().on(MessageCreateEvent.class).subscribe(event -> {
            if (CONFIG.discord.chatRelay.channelId.length() > 0 && event.getMessage().getChannelId().equals(Snowflake.of(CONFIG.discord.chatRelay.channelId))) {
                if (!event.getMember().get().getId().equals(this.client.getSelfId())) {
                    EVENT_BUS.dispatch(new DiscordMessageSentEvent(event.getMessage().getContent()));
                    return;
                }
            }
            if (!event.getMessage().getChannelId().equals(Snowflake.of(CONFIG.discord.channelId))) {
                return;
            }
            final String message = event.getMessage().getContent();
            if (!message.startsWith(CONFIG.discord.prefix)) {
                return;
            }
            RestChannel restChannel = restClient.getChannelById(event.getMessage().getChannelId());
            commands.stream()
                    .filter(command -> message.toLowerCase(Locale.ROOT).startsWith(CONFIG.discord.prefix + command.getName().toLowerCase(Locale.ROOT)))
                    .findFirst()
                    .ifPresent(command -> {
                        try {
                            MultipartRequest<MessageCreateRequest> m = command.execute(event, restChannel);
                            if (m != null) {
                                restChannel.createMessage(m).block();
                            }
                        } catch (final Exception e) {
                            DISCORD_LOG.error("Error executing discord command: " + command, e);
                        }
                    });
        });

        if (CONFIG.discord.isUpdating) {
            handleProxyUpdateComplete();
        }
        presenceExecutorService.scheduleAtFixedRate(this::updatePresence, 0L,
                15L, // discord rate limit
                TimeUnit.SECONDS);
    }

    private void updatePresence() {
        if (this.proxy.isInQueue()) {
            this.client.updatePresence(getQueuePresence()).subscribe();
        } else if (this.proxy.isConnected()) {
            this.client.updatePresence(getOnlinePresence()).subscribe();
        } else {
            this.client.updatePresence(DISCONNECTED_PRESENCE).subscribe();
        }
    }

    private ClientPresence getOnlinePresence() {
        long onlineSeconds = Instant.now().getEpochSecond() - this.proxy.getConnectTime().getEpochSecond();
        return ClientPresence.of(Status.ONLINE, ClientActivity.playing(CONFIG.client.server.address + " [" + Queue.getEtaStringFromSeconds(onlineSeconds) + "]"));
    }

    private void handleProxyUpdateComplete() {
        CONFIG.discord.isUpdating = false;
        saveConfig();
        restClient.getChannelById(Snowflake.of(CONFIG.discord.channelId)).createMessage(
                MessageCreateSpec.builder()
                        .addEmbed(EmbedCreateSpec.builder()
                                .title("Update complete!")
                                .color(Color.CYAN)
                                .build())
                        .build().asRequest())
                .subscribe();
    }

    @Subscribe
    public void handleConnectEvent(ConnectEvent event) {
        this.client.updatePresence(DEFAULT_CONNECTED_PRESENCE).subscribe();
        sendEmbedMessage(EmbedCreateSpec.builder()
               .title("Proxy Connected!" + " : " + CONFIG.authentication.username)
               .color(Color.CYAN)
               .addField("Server", CONFIG.client.server.address, true)
               .addField("Regular Queue", ""+Queue.getQueueStatus().regular, true)
               .addField("Priority Queue", ""+Queue.getQueueStatus().prio, true)
               .addField("Proxy IP", CONFIG.server.getProxyAddress(), false)
               .build());
    }

    @Subscribe
    public void handlePlayerOnlineEvent(PlayerOnlineEvent event) {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Proxy Online!" + " : " + CONFIG.authentication.username)
                .color(Color.CYAN)
                .addField("Server", CONFIG.client.server.address, true)
//                .addField("Proxy IP", CONFIG.server.getProxyAddress(), false)
                .build());
    }

    @Subscribe
    public void handleDisconnectEvent(DisconnectEvent event) {
        this.client.updatePresence(DISCONNECTED_PRESENCE).subscribe();
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Proxy Disconnected" + " : " + CONFIG.authentication.username)
                .addField("Reason", event.reason, true)
                .color(Color.CYAN)
                .build());
    }

    @Subscribe
    public void handleQueuePositionUpdateEvent(QueuePositionUpdateEvent event) {
        this.client.updatePresence(getQueuePresence()).subscribe();
        if (event.position == CONFIG.server.queueWarning) {
            sendQueueWarning(event.position);
        } else if (event.position <= 3) {
            sendQueueWarning(event.position);
        }
    }

    private void sendQueueWarning(int position) {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Proxy Queue Warning" + " : " + CONFIG.authentication.username)
                .color(this.proxy.isConnected() ? Color.CYAN : Color.RUBY)
                .addField("Server", CONFIG.client.server.address, true)
                .addField("Queue Position", "[" + queuePositionStr() + "]", false)
//                .addField("Proxy IP", CONFIG.server.getProxyAddress(), false)
                .build());

    }

    private String queuePositionStr() {
        if (proxy.getIsPrio().isPresent()) {
            if (proxy.getIsPrio().get()) {
                return this.proxy.getQueuePosition() + " / " + Queue.getQueueStatus().prio + " - ETA: " + Queue.getQueueEta(Queue.getQueueStatus().prio, this.proxy.getQueuePosition());
            } else {
                return this.proxy.getQueuePosition() + " / " + Queue.getQueueStatus().regular + " - ETA: " + Queue.getQueueEta(Queue.getQueueStatus().regular, this.proxy.getQueuePosition());
            }
        } else {
            return "?";
        }
    }

    @Subscribe
    public void handleQueueCompleteEvent(QueueCompleteEvent event) {
        this.client.updatePresence(DEFAULT_CONNECTED_PRESENCE).subscribe();
    }

    @Subscribe
    public void handleStartQueueEvent(StartQueueEvent event) {
        this.client.updatePresence(getQueuePresence()).subscribe();
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Proxy Started Queuing..." + " : " + CONFIG.authentication.username)
                .color(Color.CYAN)
                .addField("Regular Queue", ""+Queue.getQueueStatus().regular, true)
                .addField("Priority Queue", ""+Queue.getQueueStatus().prio, true)
                .build());
    }

    @Subscribe
    public void handleDeathEvent(DeathEvent event) {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Player Death!" + " : " + CONFIG.authentication.username)
                .color(Color.RUBY)
                .addField("Coordinates", getCoordinates(CACHE.getPlayerCache()), false)
                .build());
    }

    @Subscribe
    public void handleDeathMessageEvent(DeathMessageEvent event) {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Death Message")
                .color(Color.RUBY)
                .addField("Message", event.message, false)
                .build());
    }

    @Subscribe
    public void handleNewPlayerInVisualRangeEvent(NewPlayerInVisualRangeEvent event) {
        if (CONFIG.client.extra.visualRangeAlert) {
            EmbedCreateSpec.Builder embedCreateSpec = EmbedCreateSpec.builder()
                    .title("Player In Visual Range")
                    .addField("Player Name", Optional.ofNullable(event.playerEntry.getName()).orElse("Unknown"), true)
                    .addField("Player UUID", event.playerEntry.getId().toString(), true)
                    .image(this.proxy.getAvatarURL(event.playerEntry.getId()).toString());

            if (CONFIG.discord.reportCoords) {
                embedCreateSpec.addField("Coordinates", "["
                        + (int) event.playerEntity.getX() + ", "
                        + (int) event.playerEntity.getY() + ", "
                        + (int) event.playerEntity.getZ()
                        + "]", false);
            }
            if (CONFIG.client.extra.visualRangeAlertMention) {
                boolean notFriend = CONFIG.client.extra.friendList.stream()
                        .noneMatch(friend -> friend.equalsIgnoreCase(Optional.ofNullable(event.playerEntry.getName()).orElse("Unknown")));
                if (notFriend) {
                    if (CONFIG.discord.visualRangeMentionRoleId.length() > 3) {
                        sendEmbedMessage("<@&" + CONFIG.discord.visualRangeMentionRoleId + ">", embedCreateSpec.build());
                    } else {
                        sendEmbedMessage("<@&" + CONFIG.discord.accountOwnerRoleId + ">", embedCreateSpec.build());
                    }
                } else {
                    sendEmbedMessage(embedCreateSpec.build());
                }
            } else {
                sendEmbedMessage(embedCreateSpec.build());
            }
        }
    }

    @Subscribe
    public void handleAutoDisconnectEvent(AutoDisconnectEvent event) {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Proxy AutoDisconnect Triggered!" + " : " + CONFIG.authentication.username)
                .addField("Health", ""+((int)CACHE.getPlayerCache().getThePlayer().getHealth()), true)
                .color(Color.CYAN)
                .build());
    }

    @Subscribe
    public void handleProxyClientConnectedEvent(ProxyClientConnectedEvent event) {
        if (CONFIG.client.extra.clientConnectionMessages) {
            sendEmbedMessage(EmbedCreateSpec.builder()
                    .title("Client Connected")
                    .addField("Username", event.clientGameProfile.getName(), true)
                    .color(Color.CYAN)
                    .build());
        }
    }

    @Subscribe
    public void handleProxyClientDisconnectedEvent(ProxyClientDisconnectedEvent event) {
        if (CONFIG.client.extra.clientConnectionMessages) {
            EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder()
                    .title("Client Disconnected")
                    .color(Color.RUBY);
            if (nonNull(event.message)) {
                builder = builder.addField("Message", event.message, false);
            }
            sendEmbedMessage(builder
                    .build());
        }
    }

    @Subscribe
    public void handleActiveHoursConnectEvent(ActiveHoursConnectEvent event) {
        int queueLength;
        if (proxy.getIsPrio().orElse(false)) {
            queueLength = Queue.getQueueStatus().prio;
        } else {
            queueLength = Queue.getQueueStatus().regular;
        }
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Active Hours Connect Triggered")
                .addField("ETA", Queue.getQueueEta(queueLength, queueLength), false)
                .color(Color.CYAN)
                .build());
    }

    @Subscribe
    public void handleServerChatReceivedEvent(ServerChatReceivedEvent event) {
        if (CONFIG.discord.chatRelay.enable && CONFIG.discord.chatRelay.channelId.length() > 0) {
            if (CONFIG.discord.chatRelay.ignoreQueue && this.proxy.isInQueue()) return;
            try {
                String message = escape(event.message);
                if (CONFIG.discord.chatRelay.mentionRoleOnWhisper) {
                    if (!message.startsWith("<")) {
                        String[] split = message.split(" ");
                        if (split.length > 2 && split[1].startsWith("whispers")) {
                            message = "<@&" + CONFIG.discord.accountOwnerRoleId + "> " + message;
                        }
                    }
                }
                relayRestChannel.get().createMessage(message).subscribe();
            } catch (final Throwable e) {
                DISCORD_LOG.error(e);
            }
        }
    }

    @Subscribe
    public void handleServerPlayerConnectedEvent(ServerPlayerConnectedEvent event) {
        if (CONFIG.discord.chatRelay.enable && CONFIG.discord.chatRelay.connectionMessages && CONFIG.discord.chatRelay.channelId.length() > 0) {
            if (CONFIG.discord.chatRelay.ignoreQueue && this.proxy.isInQueue()) return;
            try {
                relayRestChannel.get().createMessage(escape(event.playerName + " connected")).subscribe();
            } catch (final Throwable e) {
                DISCORD_LOG.error(e);
            }
        }
    }

    @Subscribe
    public void handleServerPlayerDisconnectedEvent(ServerPlayerDisconnectedEvent event) {
        if (CONFIG.discord.chatRelay.enable && CONFIG.discord.chatRelay.connectionMessages && CONFIG.discord.chatRelay.channelId.length() > 0) {
            if (CONFIG.discord.chatRelay.ignoreQueue && this.proxy.isInQueue()) return;
            try {
                relayRestChannel.get().createMessage(escape(event.playerName + " disconnected")).subscribe();
            } catch (final Throwable e) {
                DISCORD_LOG.error(e);
            }
        }
    }

    @Subscribe
    public void handleDiscordMessageSentEvent(DiscordMessageSentEvent event) {
        if (CONFIG.discord.chatRelay.enable) {
            if (this.proxy.isConnected()) {
                this.proxy.getClient().send(new ClientChatPacket(event.message));
            }
        }
    }

    public void sendAutoReconnectMessage() {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("AutoReconnecting in " + CONFIG.client.extra.autoReconnect.delaySeconds + "s")
                .color(Color.CYAN)
                .build());
    }

    private void sendEmbedMessage(EmbedCreateSpec embedCreateSpec) {
        try {
            mainRestChannel.get().createMessage(MessageCreateSpec.builder()
                    .addEmbed(embedCreateSpec)
                    .build().asRequest()).subscribe();
        } catch (final Exception e) {
            DISCORD_LOG.error("Failed sending discord message", e);
        }
    }

    private void sendEmbedMessage(String message, EmbedCreateSpec embedCreateSpec) {
        try {
            mainRestChannel.get().createMessage(MessageCreateSpec.builder()
                    .content(message)
                    .addEmbed(embedCreateSpec)
                    .build().asRequest()).subscribe();
        } catch (final Exception e) {
            DISCORD_LOG.error("Failed sending discord message", e);
        }
    }

    private ClientPresence getQueuePresence() {
        return ClientPresence.of(Status.IDLE, ClientActivity.watching(queuePositionStr()));
    }

    public static String escape(String message) {
        return message.replaceAll("_", "\\\\_");
    }
}
