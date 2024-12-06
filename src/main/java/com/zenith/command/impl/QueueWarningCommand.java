package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.discord.Embed;
import com.zenith.module.impl.QueueWarning;

import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.MODULE;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class QueueWarningCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "queueWarning",
            CommandCategory.INFO,
            """
            Configure warnings sent when 2b2t queue positions are reached.
            
            The list of queue positions to send the warnings can be configured, each with an optional mention.
            """,
            asList(
                "on/off",
                "list",
                "clear",
                "add <position> mention",
                "del <position>"
            )
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("queueWarning")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.queueWarning.enabled = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Queue Warning " + toggleStrCaps(CONFIG.client.extra.queueWarning.enabled));
                MODULE.get(QueueWarning.class).syncEnabledFromConfig();
                return OK;
            }))
            .then(literal("list").executes(c -> {
                c.getSource().getEmbed()
                    .title("Queue Warning List");
                return OK;
            }))
            .then(literal("clear").executes(c -> {
                CONFIG.client.extra.queueWarning.warningPositions.clear();
                CONFIG.client.extra.queueWarning.mentionPositions.clear();
                c.getSource().getEmbed().title("Cleared All Positions");
                return OK;
            }))
            .then(literal("add").then(argument("pos", integer(1, 1000)).executes(c -> {
                int pos = getInteger(c, "pos");
                CONFIG.client.extra.queueWarning.warningPositions.add(pos);
                CONFIG.client.extra.queueWarning.mentionPositions.remove(pos);
                c.getSource().getEmbed().title("Added " + pos);
                return OK;
            })
                .then(literal("mention").executes(c -> {
                    int pos = getInteger(c, "pos");
                    CONFIG.client.extra.queueWarning.warningPositions.add(pos);
                    CONFIG.client.extra.queueWarning.mentionPositions.add(pos);
                    c.getSource().getEmbed().title("Added " + pos);
                    return OK;
                }))))
            .then(literal("del").then(argument("pos", integer(1, 1000)).executes(c -> {
                int pos = getInteger(c, "pos");
                CONFIG.client.extra.queueWarning.warningPositions.remove(pos);
                CONFIG.client.extra.queueWarning.mentionPositions.remove(pos);
                c.getSource().getEmbed().title("Deleted " + pos);
                return OK;
            })));
    }

    @Override
    public void postPopulate(final Embed builder) {
        builder
            .addField("Queue Warning", toggleStr(CONFIG.client.extra.queueWarning.enabled), false)
            .description("**Warn Positions**\n" + getWarnListStr())
            .primaryColor();
    }

    private String getWarnListStr() {
        return CONFIG.client.extra.queueWarning.warningPositions.isEmpty()
            ? "Empty"
            : CONFIG.client.extra.queueWarning.warningPositions.intStream()
                .sorted()
                .mapToObj(pos -> pos + (CONFIG.client.extra.queueWarning.mentionPositions.contains(pos) ? " (m)" : ""))
                .collect(Collectors.joining("\n"));
    }
}
