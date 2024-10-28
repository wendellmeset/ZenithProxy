package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.discord.Embed;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.DATABASE;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class DatabaseCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full(
            "database",
            CommandCategory.MANAGE,
            """
            Configures the database module used for https://api.2b2t.vc
            
            This is disabled by default. No ZenithProxy users contribute or collect data, this is purely for use with my own accounts.
            """,
            asList(
                "on/off",
                "queueWait on/off",
                "queueLength on/off",
                "publicChat on/off",
                "joinLeave on/off",
                "deathMessages on/off",
                "restarts on/off",
                "playerCount on/off",
                "tablist on/off",
                "playtime on/off"
            ),
            asList("db")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("database")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.database.enabled = getToggle(c, "toggle");
                if (CONFIG.database.enabled) DATABASE.start();
                else DATABASE.stop();
                c.getSource().getEmbed()
                    .title("Databases " + toggleStrCaps(CONFIG.database.enabled));
                return OK;

            }))
            .then(literal("queueWait")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.database.queueWaitEnabled = getToggle(c, "toggle");
                          if (CONFIG.database.queueWaitEnabled) DATABASE.startQueueWaitDatabase();
                          else DATABASE.stopQueueWaitDatabase();
                          c.getSource().getEmbed()
                              .title("Queue Wait Database " + toggleStrCaps(CONFIG.database.queueWaitEnabled));
                          return OK;
                      })))
            .then(literal("queueLength")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.database.queueLengthEnabled = getToggle(c, "toggle");
                          if (CONFIG.database.queueLengthEnabled) DATABASE.startQueueLengthDatabase();
                          else DATABASE.stopQueueLengthDatabase();
                          c.getSource().getEmbed()
                              .title("Queue Length Database " + toggleStrCaps(CONFIG.database.queueLengthEnabled));
                          return OK;
                      })))
            .then(literal("publicChat")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.database.chatsEnabled = getToggle(c, "toggle");
                          if (CONFIG.database.chatsEnabled) DATABASE.startChatsDatabase();
                          else DATABASE.stopChatsDatabase();
                          c.getSource().getEmbed()
                              .title("Public Chat Database " + toggleStrCaps(CONFIG.database.chatsEnabled));
                          return OK;
                      })))
            .then(literal("joinLeave")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.database.connectionsEnabled = getToggle(c, "toggle");
                          if (CONFIG.database.connectionsEnabled) DATABASE.startConnectionsDatabase();
                          else DATABASE.stopConnectionsDatabase();
                          c.getSource().getEmbed()
                              .title("Connections Database " + toggleStrCaps(CONFIG.database.connectionsEnabled));
                          return OK;
                      })))
            .then(literal("deathMessages")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.database.deathsEnabled = getToggle(c, "toggle");
                          if (CONFIG.database.deathsEnabled) DATABASE.startDeathsDatabase();
                          else DATABASE.stopDeathsDatabase();
                          c.getSource().getEmbed()
                              .title("Death Messages Database " + toggleStrCaps(CONFIG.database.deathsEnabled));
                          return OK;
                      })))
            .then(literal("restarts")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.database.restartsEnabled = getToggle(c, "toggle");
                          if (CONFIG.database.restartsEnabled) DATABASE.startRestartsDatabase();
                          else DATABASE.stopRestartsDatabase();
                          c.getSource().getEmbed()
                              .title("Restarts Database " + toggleStrCaps(CONFIG.database.restartsEnabled));
                          return OK;
                      })))
            .then(literal("playerCount")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.database.playerCountEnabled = getToggle(c, "toggle");
                          if (CONFIG.database.playerCountEnabled) DATABASE.startPlayerCountDatabase();
                          else DATABASE.stopPlayerCountDatabase();
                          c.getSource().getEmbed()
                              .title("Player Count Database " + toggleStrCaps(CONFIG.database.playerCountEnabled));
                          return OK;
                      })))
            .then(literal("tablist")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.database.tablistEnabled = getToggle(c, "toggle");
                          if (CONFIG.database.tablistEnabled) DATABASE.startTablistDatabase();
                          else DATABASE.stopTablistDatabase();
                          c.getSource().getEmbed()
                              .title("Tablist Database " + toggleStrCaps(CONFIG.database.tablistEnabled));
                          return OK;
                      })))
            .then(literal("playtime")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.database.playtimeEnabled = getToggle(c, "toggle");
                          if (CONFIG.database.playtimeEnabled) DATABASE.startPlaytimeDatabase();
                          else DATABASE.stopPlaytimeDatabase();
                          c.getSource().getEmbed()
                              .title("Playtime Database " + toggleStrCaps(CONFIG.database.playtimeEnabled));
                          return OK;
                      })));
    }

    @Override
    public void postPopulate(final Embed builder) {
        builder
            .addField("Queue Wait", toggleStr(CONFIG.database.queueWaitEnabled), false)
            .addField("Queue Length", toggleStr(CONFIG.database.queueLengthEnabled), false)
            .addField("Public Chat", toggleStr(CONFIG.database.chatsEnabled), false)
            .addField("Join/Leave", toggleStr(CONFIG.database.connectionsEnabled), false)
            .addField("Death Messages", toggleStr(CONFIG.database.deathsEnabled), false)
            .addField("Restarts", toggleStr(CONFIG.database.restartsEnabled), false)
            .addField("Player Count", toggleStr(CONFIG.database.playerCountEnabled), false)
            .addField("Tablist", toggleStr(CONFIG.database.tablistEnabled), false)
            .addField("Playtime", toggleStr(CONFIG.database.playtimeEnabled), false)
            .primaryColor();
    }
}
