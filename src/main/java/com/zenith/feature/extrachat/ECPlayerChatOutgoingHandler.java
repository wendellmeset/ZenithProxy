package com.zenith.feature.extrachat;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.data.game.chat.ChatFilterType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerChatPacket;

import static com.zenith.Shared.*;

public class ECPlayerChatOutgoingHandler implements PacketHandler<ClientboundPlayerChatPacket, ServerSession> {

    @Override
    public ClientboundPlayerChatPacket apply(final ClientboundPlayerChatPacket packet, final ServerSession session) {
        var chatType = CACHE.getChatCache().getChatTypeRegistry().getChatType(packet.getChatType().id());
        if (chatType != null) {
            boolean isWhisper = "commands.message.display.incoming".equals(chatType.translationKey()) || "commands.message.display.outgoing".equals(chatType.translationKey());
            if (CONFIG.client.extra.chat.hideWhispers && isWhisper) {
                packet.setFilterMask(ChatFilterType.FULLY_FILTERED);
                return packet;
            } else if (CONFIG.client.extra.chat.hideChat && !isWhisper) {
                packet.setFilterMask(ChatFilterType.FULLY_FILTERED);
                return packet;
            }
        } else {
            SERVER_LOG.warn("Unknown chat type: {}", packet.getChatType().id());
        }
        return packet;
    }
}
