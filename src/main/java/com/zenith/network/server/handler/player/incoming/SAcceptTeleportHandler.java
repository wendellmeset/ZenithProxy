package com.zenith.network.server.handler.player.incoming;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;

import java.util.Optional;

import static com.zenith.Shared.SERVER_LOG;

public class SAcceptTeleportHandler implements PacketHandler<ServerboundAcceptTeleportationPacket, ServerSession> {
    @Override
    public ServerboundAcceptTeleportationPacket apply(final ServerboundAcceptTeleportationPacket packet, final ServerSession session) {
        if (session.isSpawned()) return packet;
        else {
            if (session.getSpawnTeleportId() == packet.getId()) {
                SERVER_LOG.debug("[{}] Accepted spawn teleport", Optional.ofNullable(session.getProfileCache().getProfile()).map(GameProfile::getName).orElse("?"));
                session.setSpawning(true);
            } else {
                SERVER_LOG.debug("[{}] Cancelling unexpected pre-spawn teleport packet with ID: {}", Optional.ofNullable(session.getProfileCache().getProfile()).map(GameProfile::getName).orElse("?"), packet.getId());
            }
            return null;
        }
    }
}
