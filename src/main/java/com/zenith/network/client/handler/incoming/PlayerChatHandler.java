package com.zenith.network.client.handler.incoming;

import com.zenith.event.proxy.ServerChatReceivedEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;
import com.zenith.util.ComponentSerializer;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerChatPacket;

import java.util.Optional;

import static com.zenith.Shared.*;

public class PlayerChatHandler implements PacketHandler<ClientboundPlayerChatPacket, ClientSession> {

    @Override
    public ClientboundPlayerChatPacket apply(ClientboundPlayerChatPacket packet, ClientSession session) {
        // we shouldn't receive any of these packets on 2b or any anarchy server due to no chat reports plugins
        // and this does not pass these packets through to our normal chat handlers
        // so no chat relay, chat events, and other stuff
        var senderPlayerEntry = CACHE.getTabListCache().get(packet.getSender());
        var chatType = CACHE.getChatCache().getChatTypeRegistry().getChatType(packet.getChatType().id());
        Component chatComponent = chatType.render(
            packet.getName(),
            Component.text(packet.getContent()),
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
