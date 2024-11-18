package com.zenith.event.proxy.chat;

import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;

public record PublicChatEvent(
    PlayerListEntry sender,
    Component component,
    // note: this has the preceding sender data stripped: `<sender> `
    String message
) { }
