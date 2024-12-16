package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.discord.Embed;
import com.zenith.module.impl.Wander;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.MODULE;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class WanderCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "wander",
            CommandCategory.MODULE,
            """
            Randomly moves the player around in the world
            """,
            asList(
                "on/off",
                "turn on/off",
                "turn delay <seconds>",
                "jump on/off",
                "jump delay <seconds>",
                "jump inWater on/off",
                "sneak on/off"
            )
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("wander")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.wander.enabled = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Wander " + toggleStrCaps(CONFIG.client.extra.wander.enabled));
                MODULE.get(Wander.class).syncEnabledFromConfig();
                return OK;
            }))
            .then(literal("turn")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.wander.turn = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Turn " + toggleStrCaps(CONFIG.client.extra.wander.turn));
                            return OK;
                        }))
                      .then(literal("delay").then(argument("turnDelaySeconds", integer(1, 1000)).executes(c -> {
                            CONFIG.client.extra.wander.turnDelaySeconds = getInteger(c, "turnDelaySeconds");
                            c.getSource().getEmbed()
                                .title("Turn Delay Seconds Set");
                            return OK;
                      }))))
            .then(literal("jump")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.wander.jump = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Jump " + toggleStrCaps(CONFIG.client.extra.wander.jump));
                            return OK;
                      }))
                      .then(literal("delay").then(argument("jumpDelaySeconds", integer(1, 1000)).executes(c -> {
                            CONFIG.client.extra.wander.jumpDelaySeconds = getInteger(c, "jumpDelaySeconds");
                            c.getSource().getEmbed()
                                .title("Jump Delay Seconds Set");
                            return OK;
                      })))
                      .then(literal("inWater").then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.wander.alwaysJumpInWater = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Jump In Water " + toggleStrCaps(CONFIG.client.extra.wander.alwaysJumpInWater));
                            return OK;
                      }))))
            .then(literal("sneak").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.wander.sneak = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Sneak " + toggleStrCaps(CONFIG.client.extra.wander.sneak));
                return OK;
            })));
    }

    @Override
    public void postPopulate(Embed embed) {
        embed
            .addField("Wander", toggleStr(CONFIG.client.extra.wander.enabled), false)
            .addField("Turn", toggleStr(CONFIG.client.extra.wander.turn), false)
            .addField("Turn Delay Seconds", CONFIG.client.extra.wander.turnDelaySeconds, false)
            .addField("Jump", toggleStr(CONFIG.client.extra.wander.jump), false)
            .addField("Jump Delay Seconds", CONFIG.client.extra.wander.jumpDelaySeconds, false)
            .addField("Jump In Water", toggleStr(CONFIG.client.extra.wander.alwaysJumpInWater), false)
            .addField("Sneak", toggleStr(CONFIG.client.extra.wander.sneak), false)
            .primaryColor();
    }
}
