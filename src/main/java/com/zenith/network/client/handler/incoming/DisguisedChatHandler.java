package com.zenith.network.client.handler.incoming;

import com.zenith.cache.data.chat.ChatType;
import com.zenith.event.proxy.ServerChatReceivedEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;
import com.zenith.util.ComponentSerializer;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundDisguisedChatPacket;

import java.util.Optional;

import static com.zenith.Shared.*;

public class DisguisedChatHandler implements PacketHandler<ClientboundDisguisedChatPacket, ClientSession> {
    @Override
    public ClientboundDisguisedChatPacket apply(final ClientboundDisguisedChatPacket packet, final ClientSession session) {
        var senderPlayerEntry = CACHE.getTabListCache().getFromName(ComponentSerializer.serializePlain(packet.getName()));
        ChatType chatType = CACHE.getChatCache().getChatTypeRegistry().getChatType(packet.getChatType().id());
        Component chatComponent = chatType.render(
            packet.getName(),
            packet.getMessage(),
            packet.getTargetName());
        if (CONFIG.client.extra.logChatMessages) {
            CLIENT_LOG.info("{}", ComponentSerializer.serializeJson(chatComponent));
        }
        Optional<PlayerListEntry> whisperTarget = Optional.empty();
        if ("commands.message.display.incoming".equals(chatType.translationKey())) {
            whisperTarget = CACHE.getTabListCache().get(CACHE.getProfileCache().getProfile().getId());
        } else if ("commands.message.display.outgoing".equals(chatType.translationKey())) {
            whisperTarget = CACHE.getTabListCache().getFromName( // ???
                ComponentSerializer.serializePlain(packet.getTargetName())
            );
        }
        EVENT_BUS.postAsync(new ServerChatReceivedEvent(
            senderPlayerEntry,
            chatComponent,
            ComponentSerializer.serializePlain(chatComponent),
            whisperTarget,
            Optional.empty()
        ));
        return packet;
    }
}
