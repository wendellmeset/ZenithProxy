package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.discord.Embed;
import com.zenith.feature.api.prioban.PriobanApi;

import static com.zenith.Shared.CONFIG;
import static com.zenith.command.brigadier.CustomStringArgumentType.getString;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class PrioCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "prio",
            CommandCategory.INFO,
            "Configure alerts for 2b2t priority queue status and bans",
            asList(
                "mentions on/off",
                "banMentions on/off",
                "banCheck",
                "banCheck <playerName>"
            )
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("prio")
            .then(literal("mentions")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.discord.mentionRoleOnPrioUpdate = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Prio Mentions " + toggleStrCaps(CONFIG.discord.mentionRoleOnPrioUpdate));
                            return OK;
                        })))
            .then(literal("banMentions")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.discord.mentionRoleOnPrioBanUpdate = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Prio Ban Mentions " + toggleStrCaps(CONFIG.discord.mentionRoleOnPrioBanUpdate));
                            return OK;
                        })))
            .then(literal("banCheck")
                      .then(argument("name", wordWithChars()).executes(c -> {
                          String playerName = getString(c, "name");
                          String status = PriobanApi.INSTANCE.checkPrioBan(playerName)
                              .map(Object::toString)
                              .orElse("unknown");
                          c.getSource().getEmbed()
                              .title("Checking Prio ban")
                              .addField("Banned", status, true);
                          return OK;
                      }))
                      .executes(c -> {
                          String status = PriobanApi.INSTANCE.checkPrioBan(CONFIG.authentication.username)
                              .map(Object::toString)
                              .orElse("unknown");
                          c.getSource().getEmbed()
                              .title("Checking Prio ban")
                              .addField("Banned", status, true);
                          return OK;
                      }));
    }

    @Override
    public void postPopulate(Embed builder) {
        builder
            .addField("Prio Status Mentions", toggleStr(CONFIG.discord.mentionRoleOnPrioUpdate), true)
            .addField("Prio Ban Mentions", toggleStr(CONFIG.discord.mentionRoleOnPrioBanUpdate), true)
            .primaryColor();
    }
}
