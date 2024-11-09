package com.zenith.network.client.handler.outgoing;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandSignedPacket;

import java.util.BitSet;

import static com.zenith.Shared.CACHE;

public class OutgoingChatCommandSignedHandler implements PacketHandler<ServerboundChatCommandSignedPacket, ClientSession> {
    @Override
    public ServerboundChatCommandSignedPacket apply(final ServerboundChatCommandSignedPacket packet, final ClientSession session) {
        final var cacheTimestamp = CACHE.getChatCache().getLastChatTimestamp();
        if (packet.getTimeStamp() < cacheTimestamp) packet.setTimeStamp(cacheTimestamp);
        CACHE.getChatCache().setLastChatTimestamp(packet.getTimeStamp());
        if (CACHE.getChatCache().canUseChatSigning()) {
            String command = packet.getCommand().split(" ")[0].toLowerCase();
            if ("w".equals(command) || "whisper".equals(command) || "msg".equals(command) || "tell".equals(command)) {
                var signedWhisper = new ServerboundChatCommandSignedPacket(packet.getCommand(), packet.getTimeStamp(), 0, packet.getSignatures(), 0, BitSet.valueOf(new byte[20]));
                CACHE.getChatCache().getChatSession().sign(signedWhisper);
                return signedWhisper;
            }
        }
        return packet;
    }
}
