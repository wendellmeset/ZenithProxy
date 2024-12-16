package com.zenith.module.impl;

import com.zenith.Proxy;
import com.zenith.event.module.PlayerHealthChangedEvent;
import com.zenith.event.module.WeatherChangeEvent;
import com.zenith.event.proxy.HealthAutoDisconnectEvent;
import com.zenith.event.proxy.NewPlayerInVisualRangeEvent;
import com.zenith.event.proxy.ProxyClientDisconnectedEvent;
import com.zenith.event.proxy.TotemPopEvent;
import com.zenith.module.Module;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.*;
import static java.util.Objects.nonNull;

public class AutoDisconnect extends Module {
    public static final String AUTODISCONNECT_REASON_PREFIX = "[AutoDisconnect] ";

    public AutoDisconnect() {
        super();
    }

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(
            this,
            of(PlayerHealthChangedEvent.class, this::handleLowPlayerHealthEvent),
            of(WeatherChangeEvent.class, this::handleWeatherChangeEvent),
            of(ProxyClientDisconnectedEvent.class, this::handleProxyClientDisconnectedEvent),
            of(NewPlayerInVisualRangeEvent.class, this::handleNewPlayerInVisualRangeEvent),
            of(TotemPopEvent.class, this::handleTotemPopEvent)
        );
    }

    @Override
    public boolean enabledSetting() {
        return CONFIG.client.extra.utility.actions.autoDisconnect.enabled;
    }

    public void handleLowPlayerHealthEvent(final PlayerHealthChangedEvent event) {
        if (!CONFIG.client.extra.utility.actions.autoDisconnect.healthDisconnect) return;
        if (event.newHealth() <= CONFIG.client.extra.utility.actions.autoDisconnect.health
            && playerConnectedCheck()) {
            info("Health: {} < {}",
                 event.newHealth(),
                 CONFIG.client.extra.utility.actions.autoDisconnect.health);
            EVENT_BUS.postAsync(new HealthAutoDisconnectEvent());
            doDisconnect("Health: " + event.newHealth() + " <= " + CONFIG.client.extra.utility.actions.autoDisconnect.health);
        }
    }

    public void handleWeatherChangeEvent(final WeatherChangeEvent event) {
        if (!CONFIG.client.extra.utility.actions.autoDisconnect.thunder) return;
        if (CACHE.getChunkCache().isRaining()
            && CACHE.getChunkCache().getThunderStrength() > 0.0f
            && playerConnectedCheck()) {
            info("Thunder disconnect");
            doDisconnect("Thunder");
        }
    }

    public void handleProxyClientDisconnectedEvent(ProxyClientDisconnectedEvent event) {
        if (!CONFIG.client.extra.utility.actions.autoDisconnect.autoClientDisconnect) return;
        var connection = Proxy.getInstance().getActivePlayer();
        if (nonNull(connection) && connection.getProfileCache().getProfile().equals(event.clientGameProfile())) {
            info("Auto Client Disconnect");
            doDisconnect("Auto Client Disconnect");
        }
    }

    public void handleNewPlayerInVisualRangeEvent(NewPlayerInVisualRangeEvent event) {
        if (!CONFIG.client.extra.utility.actions.autoDisconnect.onUnknownPlayerInVisualRange) return;
        var playerUUID = event.playerEntity().getUuid();
        if (PLAYER_LISTS.getFriendsList().contains(playerUUID)
            || PLAYER_LISTS.getWhitelist().contains(playerUUID)
            || PLAYER_LISTS.getSpectatorWhitelist().contains(playerUUID)
            || !playerConnectedCheck()
        ) return;
        info("Unknown Player: {} [{}]", event.playerEntry().getProfile());
        doDisconnect("Unknown Player: " + event.playerEntry().getProfile().getName());
    }

    private void handleTotemPopEvent(TotemPopEvent event) {
        if (!CONFIG.client.extra.utility.actions.autoDisconnect.onTotemPop) return;
        if (event.entityId() != CACHE.getPlayerCache().getEntityId()) return;
        if (playerConnectedCheck()) {
            info("Totem popped");
            doDisconnect("Totem Pop");
        }
    }

    private boolean playerConnectedCheck() {
        if (Proxy.getInstance().hasActivePlayer()) {
            var whilePlayerConnected = CONFIG.client.extra.utility.actions.autoDisconnect.whilePlayerConnected;
            if (!whilePlayerConnected)
                debug("Not disconnecting because a player is connected and whilePlayerConnected setting is disabled");
            return whilePlayerConnected;
        }
        return true;
    }

    private void doDisconnect(String reason) {
        Proxy.getInstance().disconnect(AUTODISCONNECT_REASON_PREFIX + reason);
    }

    public static boolean isAutoDisconnectReason(String reason) {
        return reason.startsWith(AUTODISCONNECT_REASON_PREFIX);
    }
}
