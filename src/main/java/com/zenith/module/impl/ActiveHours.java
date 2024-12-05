package com.zenith.module.impl;

import com.zenith.Proxy;
import com.zenith.event.proxy.ActiveHoursConnectEvent;
import com.zenith.feature.queue.Queue;
import com.zenith.module.Module;
import org.jetbrains.annotations.Nullable;

import java.time.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.zenith.Shared.*;

public class ActiveHours extends Module {
    public static final String ACTIVE_HOURS_DISCONNECT_PREFIX = "[Active Hours] ";
    private @Nullable ScheduledFuture<?> activeHoursTickFuture;
    private Instant lastActiveHoursConnect = Instant.EPOCH;

    @Override
    public void subscribeEvents() {}

    @Override
    public boolean enabledSetting() {
        return CONFIG.client.extra.utility.actions.activeHours.enabled;
    }

    @Override
    public void onEnable() {
        activeHoursTickFuture = EXECUTOR.scheduleAtFixedRate(this::handleActiveHoursTick, 0L, 1L, TimeUnit.MINUTES);
    }

    @Override
    public void onDisable() {
        if (activeHoursTickFuture != null) {
            activeHoursTickFuture.cancel(false);
        }
    }

    private void handleActiveHoursTick() {
        try {
            var activeHoursConfig = CONFIG.client.extra.utility.actions.activeHours;
            var proxy = Proxy.getInstance();
            if (proxy.isOn2b2t() && (proxy.isPrio() && proxy.isConnected())) return;
            if (proxy.hasActivePlayer() && !activeHoursConfig.forceReconnect) return;
            if (lastActiveHoursConnect.isAfter(Instant.now().minus(Duration.ofHours(1)))) return;

            var queueLength = proxy.isOn2b2t()
                ? proxy.isPrio()
                ? Queue.getQueueStatus().prio()
                : Queue.getQueueStatus().regular()
                : 0;
            var queueWaitSeconds = activeHoursConfig.queueEtaCalc ? Queue.getQueueWait(queueLength) : 0;
            var nowPlusQueueWait = LocalDateTime.now(ZoneId.of(activeHoursConfig.timeZoneId))
                .plusSeconds(queueWaitSeconds)
                .atZone(ZoneId.of(activeHoursConfig.timeZoneId))
                .toInstant();
            Map<Instant, ActiveTime> activeTimesMap = new HashMap<>();
            for (ActiveTime time : activeHoursConfig.activeTimes) {
                var activeHourToday = ZonedDateTime.of(
                    LocalDate.now(ZoneId.of(activeHoursConfig.timeZoneId)),
                    LocalTime.of(time.hour(), time.minute()),
                    ZoneId.of(activeHoursConfig.timeZoneId));
                var activeHourTomorrow = activeHourToday.plusDays(1L);
                activeTimesMap.put(activeHourToday.toInstant(), time);
                activeTimesMap.put(activeHourTomorrow.toInstant(), time);
            }
            var timeRange = queueWaitSeconds > 28800 // extend range if queue wait is very long
                ? Duration.ofMinutes(15) // x2
                : Duration.ofMinutes(5);
            for (var activeTimeEntry : activeTimesMap.entrySet()) {
                Instant activeTimeInstant = activeTimeEntry.getKey();
                ActiveTime activeTime = activeTimeEntry.getValue();
                if (nowPlusQueueWait.isAfter(activeTimeInstant.minus(timeRange))
                    && nowPlusQueueWait.isBefore(activeTimeInstant.plus(timeRange))) {
                    info("Connect triggered for registered time: {}", activeTime);
                    EVENT_BUS.postAsync(new ActiveHoursConnectEvent(proxy.isConnected() && proxy.isOn2b2t()));
                    this.lastActiveHoursConnect = Instant.now();
                    if (proxy.isConnected()) {
                        proxy.disconnect(ACTIVE_HOURS_DISCONNECT_PREFIX + "Registered Time: " + activeTime);
                        if (proxy.isOn2b2t()) {
                            info("Waiting 1 minute to avoid reconnect queue skip");
                            MODULE.get(AutoReconnect.class).scheduleAutoReconnect(60);
                            return;
                        }
                    }
                    proxy.connectAndCatchExceptions();
                    return;
                }
            }
        } catch (final Exception e) {
            error("Error in active hours tick", e);
        }
    }

    public record ActiveTime(int hour, int minute) {

        public static ActiveTime fromString(final String arg) {
            final String[] split = arg.split(":");
            final int hour = Integer.parseInt(split[0]);
            final int minute = Integer.parseInt(split[1]);
            return new ActiveTime(hour, minute);
        }

        @Override
        public String toString() {
            return (hour() < 10 ? "0" + hour() : hour()) + ":" + (minute() < 10 ? "0" + minute() : minute());
        }
    }

    public static boolean isActiveHoursDisconnect(final String message) {
        return message.startsWith(ACTIVE_HOURS_DISCONNECT_PREFIX);
    }
}
