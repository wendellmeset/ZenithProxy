package com.zenith.module.impl;

import com.zenith.event.module.ClientBotTick;
import com.zenith.feature.world.Input;
import com.zenith.feature.world.Pathing;
import com.zenith.module.Module;
import com.zenith.util.Timer;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.*;

public class Wander extends Module {
    private final Timer jumpTimer = Timer.createTickTimer();
    private final Timer turnTimer = Timer.createTickTimer();
    public static final int MOVEMENT_PRIORITY = 1337;

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(
            this,
            of(ClientBotTick.class, this::handleBotTick),
            of(ClientBotTick.Starting.class, this::handleBotTickStarting)
        );
    }

    @Override
    public boolean enabledSetting() {
        return CONFIG.client.extra.wander.enabled;
    }

    private void handleBotTickStarting(ClientBotTick.Starting starting) {
        jumpTimer.reset();
        turnTimer.reset();
    }

    private void handleBotTick(ClientBotTick clientBotTick) {
        Input defaultInput = CONFIG.client.extra.wander.sneak ? Pathing.forwardSneakInput : Pathing.forwardInput;
        if (CONFIG.client.extra.wander.turn && turnTimer.tick(20L * CONFIG.client.extra.wander.turnDelaySeconds)) {
            PATHING.moveRot(defaultInput, (float) (Math.random() * 360), 0, MOVEMENT_PRIORITY);
        } else if (CONFIG.client.extra.wander.jump && jumpTimer.tick(20L * CONFIG.client.extra.wander.jumpDelaySeconds)) {
            var input = new Input(Pathing.forwardInput);
            input.jumping = true;
            PATHING.move(input, MOVEMENT_PRIORITY);
        } else if (CONFIG.client.extra.wander.jump && CONFIG.client.extra.wander.alwaysJumpInWater && MODULE.get(PlayerSimulation.class).isTouchingWater()) {
            var input = new Input(Pathing.forwardInput);
            input.jumping = true;
            PATHING.move(input, MOVEMENT_PRIORITY);
        } else {
            PATHING.move(defaultInput, MOVEMENT_PRIORITY);
        }
    }
}
