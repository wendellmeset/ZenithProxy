package com.zenith.module.impl;

import com.zenith.discord.DiscordBot;
import com.zenith.event.module.QueueWarningEvent;
import com.zenith.event.proxy.QueuePositionUpdateEvent;
import com.zenith.module.Module;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.EVENT_BUS;

public class QueueWarning extends Module {
    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(
            this,
            of(QueuePositionUpdateEvent.class, this::onQueuePositionUpdate)
        );
    }

    @Override
    public boolean enabledSetting() {
        return CONFIG.client.extra.queueWarning.enabled;
    }

    private void onQueuePositionUpdate(QueuePositionUpdateEvent event) {
        if (CONFIG.client.extra.queueWarning.warningPositions.contains(event.position())) {
            var mention = CONFIG.client.extra.queueWarning.mentionPositions.contains(event.position());
            warn("Queue Position: " + DiscordBot.queuePositionStr());
            EVENT_BUS.postAsync(new QueueWarningEvent(event.position(), mention));
        }
    }
}
