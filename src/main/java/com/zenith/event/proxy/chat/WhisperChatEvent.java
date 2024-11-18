package com.zenith.event.proxy.chat;

import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;

public record WhisperChatEvent(
    boolean outgoing,
    PlayerListEntry sender,
    PlayerListEntry receiver,
    Component component,
    String message
) { }
