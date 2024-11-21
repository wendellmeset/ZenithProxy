package com.zenith.network.server.handler.player.incoming;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;

import java.util.Optional;

import static com.zenith.Shared.SERVER_LOG;

public class SPlayerPositionRotHandler implements PacketHandler<ServerboundMovePlayerPosRotPacket, ServerSession> {
    @Override
    public ServerboundMovePlayerPosRotPacket apply(final ServerboundMovePlayerPosRotPacket packet, final ServerSession session) {
        if (session.isSpawned()) return packet;
        else {
            if (session.isSpawning()) {
                SERVER_LOG.debug("[{}] Accepted spawn position", Optional.ofNullable(session.getProfileCache().getProfile()).map(GameProfile::getName).orElse("?"));
                session.setSpawned(true);
                session.setSpawning(false);
            } else {
                SERVER_LOG.debug("[{}] Cancelling pre-spawn position packet: {}", Optional.ofNullable(session.getProfileCache().getProfile()).map(GameProfile::getName).orElse("?"), packet);
            }
            return null;
        }
    }
}
