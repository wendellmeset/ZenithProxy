package com.zenith.event.proxy.chat;

import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;

public record PublicChatEvent(
    PlayerListEntry sender,
    Component component,
    String message
) {

    // note: some servers have custom chat formatting
    //  this is the default vanilla schema and is what's used on 2b2t
    public boolean isDefaultMessageSchema() {
        return message.startsWith("<" + sender.getName() + ">");
    }

    // cuts off the sender part of the chat message
    public String extractMessageDefaultSchema() {
        return message.substring(message.indexOf(">") + 2);
    }
}
