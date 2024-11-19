package com.zenith.network.server.handler.shared.incoming;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.zenith.feature.ratelimiter.RateLimiter;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.data.handshake.HandshakeIntent;
import org.geysermc.mcprotocollib.protocol.packet.handshake.serverbound.ClientIntentionPacket;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.SERVER_LOG;

public class IntentionHandler implements PacketHandler<ClientIntentionPacket, ServerSession> {
    private final RateLimiter rateLimiter = new RateLimiter(CONFIG.server.rateLimiter.rateLimitSeconds);

    @Override
    public ClientIntentionPacket apply(final ClientIntentionPacket packet, final ServerSession session) {
        MinecraftProtocol protocol = session.getPacketProtocol();
        session.setProtocolVersion(packet.getProtocolVersion());
        session.setConnectingServerAddress(packet.getHostname());
        session.setConnectingServerPort(packet.getPort());
        if (mismatchedConnectingAddress(packet, session)) return null;
        switch (packet.getIntent()) {
            case STATUS -> {
                protocol.setOutboundState(ProtocolState.STATUS);
                session.switchInboundState(ProtocolState.STATUS);
                if (!CONFIG.server.ping.enabled) {
                    session.disconnect("bye");
                }
            }
            case LOGIN -> {
                if (handleLogin(packet, session, protocol)) return null;
            }
            case TRANSFER -> {
                SERVER_LOG.info("Transfer request from {}", session.getRemoteAddress());
                session.setTransferring(true);
                if (!CONFIG.server.acceptTransfers) {
                    session.disconnect("Transfers are disabled.");
                    return null;
                }
                if (handleLogin(packet, session, protocol)) return null;
            }
            default -> session.disconnect("Invalid client intention: " + packet.getIntent());
        }
        return null;
    }

    private static boolean mismatchedConnectingAddress(final ClientIntentionPacket packet, final ServerSession session) {
        if (!CONFIG.server.enforceMatchingConnectingAddress) return false;
        // special handling in here is related to how the mc client handles srv records and intents
        var hostname = packet.getHostname();
        if (packet.getIntent() == HandshakeIntent.LOGIN && hostname.endsWith(".")) {
            // remove trailing dot
            hostname = packet.getHostname().substring(0, packet.getHostname().length() - 1);
        }
        boolean hostnameMatch = CONFIG.server.getProxyAddressForTransfer().equals(hostname);
        boolean portMatch = CONFIG.server.getProxyPortForTransfer() == packet.getPort();
        if (packet.getIntent() == HandshakeIntent.STATUS) {
            portMatch = portMatch || 25565 == packet.getPort();
        }
        if (!hostnameMatch || !portMatch) {
            SERVER_LOG.info(
                "Disconnecting {} [{}] with intent: {} due to mismatched connecting server address. Expected: {} Actual: {}",
                session.getRemoteAddress(),
                ProtocolVersion.getProtocol(session.getProtocolVersion()).getName(),
                packet.getIntent(),
                CONFIG.server.getProxyAddressForTransfer() + ":" + CONFIG.server.getProxyPortForTransfer(),
                hostname + ":" + packet.getPort());
            session.disconnect("bye");
            return true;
        }
        return false;
    }

    private boolean handleLogin(final ClientIntentionPacket packet, final ServerSession session, final MinecraftProtocol protocol) {
        protocol.setOutboundState(ProtocolState.LOGIN);
        if (CONFIG.server.rateLimiter.enabled && rateLimiter.isRateLimited(session)) {
            SERVER_LOG.info("Disconnecting {} due to rate limiting.", session.getRemoteAddress());
            session.disconnect("Login Rate Limited.");
            return true;
        }
        if (packet.getProtocolVersion() > protocol.getCodec().getProtocolVersion()) {
            SERVER_LOG.info("Disconnecting {} due to outdated server version.", session.getRemoteAddress());
            session.disconnect("Outdated server! I'm still on " + protocol.getCodec()
                .getMinecraftVersion() + ".");
            return true;
        } else if (packet.getProtocolVersion() < protocol.getCodec().getProtocolVersion()) {
            SERVER_LOG.info("Disconnecting {} due to outdated client version.", session.getRemoteAddress());
            session.disconnect("Outdated client! Please use " + protocol.getCodec()
                .getMinecraftVersion() + ".");
            return true;
        }
        session.switchInboundState(ProtocolState.LOGIN);
        return false;
    }
}
