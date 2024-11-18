package com.zenith.module.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zenith.Proxy;
import com.zenith.event.proxy.chat.WhisperChatEvent;
import com.zenith.module.Module;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.*;
import static java.util.Objects.isNull;

public class AutoReply extends Module {
    private Cache<String, String> repliedPlayersCache = CacheBuilder.newBuilder()
            .expireAfterWrite(CONFIG.client.extra.autoReply.cooldownSeconds, TimeUnit.SECONDS)
            .build();
    private Instant lastReply = Instant.now();

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(
            this,
            of(WhisperChatEvent.class, this::handleWhisperChatEvent)
        );
    }

    @Override
    public boolean enabledSetting() {
        return CONFIG.client.extra.autoReply.enabled;
    }

    public void updateCooldown(final int newCooldown) {
        CONFIG.client.extra.autoReply.cooldownSeconds = newCooldown;
        Cache<String, String> newCache = CacheBuilder.newBuilder()
                .expireAfterWrite(newCooldown, TimeUnit.SECONDS)
                .build();
        newCache.putAll(this.repliedPlayersCache.asMap());
        this.repliedPlayersCache = newCache;
    }

    private void handleWhisperChatEvent(WhisperChatEvent event) {
        if (Proxy.getInstance().hasActivePlayer()) return;
        if (event.outgoing()) return;
        try {
            if (!event.sender().getName().equalsIgnoreCase(CONFIG.authentication.username)
                && Instant.now().minus(Duration.ofSeconds(1)).isAfter(lastReply)
                && (DISCORD.lastRelaymessage.isEmpty()
                || Instant.now().minus(Duration.ofSeconds(CONFIG.client.extra.autoReply.cooldownSeconds)).isAfter(DISCORD.lastRelaymessage.get()))) {
                if (isNull(repliedPlayersCache.getIfPresent(event.sender().getName()))) {
                    repliedPlayersCache.put(event.sender().getName(), event.sender().getName());
                    // 236 char max ( 256 - 4(command) - 16(max name length) )
                    sendClientPacketAsync(new ServerboundChatPacket("/w " + event.sender().getName() + " " + CONFIG.client.extra.autoReply.message.substring(0, Math.min(CONFIG.client.extra.autoReply.message.length(), 236))));
                    this.lastReply = Instant.now();
                }
            }
        } catch (final Throwable e) {
            CLIENT_LOG.error("AutoReply Failed", e);
        }
    }
}
