package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.feature.world.Input;
import com.zenith.module.impl.PlayerSimulation;

import static com.zenith.Shared.MODULE;
import static com.zenith.Shared.PATHING;
import static java.util.Arrays.asList;

public class ClickCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "click",
            CommandCategory.MODULE,
            """
            Clicks the block in front of you
            """,
            asList(
                "hold",
                "stop",
                "left"
//                "right"
            )
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("click")
            .then(literal("hold").executes(c -> {
                c.getSource().setNoOutput(true);
                if (!Proxy.getInstance().isConnected()) return OK;
                MODULE.get(PlayerSimulation.class).holdLeftClickOverride = true;
                return OK;
            }))
            .then(literal("stop").executes(c -> {
                c.getSource().setNoOutput(true);
                if (!Proxy.getInstance().isConnected()) return OK;
                MODULE.get(PlayerSimulation.class).holdLeftClickOverride = false;
                return OK;
            }))
            .then(literal("left").executes(c -> {
                c.getSource().setNoOutput(true);
                if (!Proxy.getInstance().isConnected()) return 1;
                var input = new Input();
                input.leftClick = true;
                PATHING.move(input, 100000);
                return 1;
            }))
            .then(literal("right").executes(c -> {
                c.getSource().setNoOutput(true);
                if (!Proxy.getInstance().isConnected()) return 1;
                var input = new Input();
                input.rightClick = true;
                PATHING.move(input, 100000);
                return 1;
            }));
    }
}
