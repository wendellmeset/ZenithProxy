package com.zenith.feature.extrachat;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandSignedPacket;

public class ECSignedChatCommandIncomingHandler implements PacketHandler<ServerboundChatCommandSignedPacket, ServerSession> {
    @Override
    public ServerboundChatCommandSignedPacket apply(final ServerboundChatCommandSignedPacket packet, final ServerSession session) {
        if (session.isSpectator()) return packet;
        final String command = packet.getCommand();
        if (command.isBlank()) return packet;
        return ECChatCommandIncomingHandler.handleExtraChatCommand(command, session) ? packet : null;
    }
}
