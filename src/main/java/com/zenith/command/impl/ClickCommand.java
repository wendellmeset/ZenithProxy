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
//                "right" // todo: implement right click
            )
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("click")
            .then(literal("hold").executes(c -> {
                if (!Proxy.getInstance().isConnected()) {
                    c.getSource().getEmbed()
                        .title("Not Connected")
                        .errorColor();
                    return OK;
                }
                MODULE.get(PlayerSimulation.class).holdLeftClickOverride = true;
                c.getSource().getEmbed()
                    .title("Click Hold On")
                    .primaryColor();
                return OK;
            }))
            .then(literal("stop").executes(c -> {
                if (!Proxy.getInstance().isConnected()) {
                    c.getSource().getEmbed()
                        .title("Not Connected")
                        .errorColor();
                    return OK;
                }
                MODULE.get(PlayerSimulation.class).holdLeftClickOverride = false;
                c.getSource().getEmbed()
                    .title("Click Hold Off")
                    .primaryColor();
                return OK;
            }))
            .then(literal("left").executes(c -> {
                if (!Proxy.getInstance().isConnected()) {
                    c.getSource().getEmbed()
                        .title("Not Connected")
                        .errorColor();
                    return OK;
                }
                var input = new Input();
                input.leftClick = true;
                PATHING.move(input, 100000);
                c.getSource().getEmbed()
                    .title("Left Clicked")
                    .primaryColor();
                return 1;
            }))
            .then(literal("right").executes(c -> {
                if (!Proxy.getInstance().isConnected()) {
                    c.getSource().getEmbed()
                        .title("Not Connected")
                        .errorColor();
                    return OK;
                }
                var input = new Input();
                input.rightClick = true;
                PATHING.move(input, 100000);
                c.getSource().getEmbed()
                    .title("Right Clicked")
                    .primaryColor();
                return 1;
            }));
    }
}
