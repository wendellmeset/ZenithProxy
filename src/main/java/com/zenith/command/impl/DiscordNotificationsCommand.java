package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.discord.Embed;

import static com.zenith.Shared.CONFIG;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class DiscordNotificationsCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full(
            "discordNotifications",
            CommandCategory.INFO,
            """
            Configures various discord notifications regarding player and proxy connections, deaths, and more.
            """,
            asList(
                "connect mention on/off",
                "online mention on/off",
                "disconnect mention on/off",
                "startQueue mention on/off",
                "death mention on/off",
                "serverRestart mention on/off",
                "loginFailed mention on/off",
                "clientConnect mention on/off",
                "clientDisconnect mention on/off",
                "spectatorConnect mention on/off",
                "spectatorDisconnect mention on/off",
                "nonWhitelistedConnect mention on/off"
            ),
            asList("on/off")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("discordNotifications")
            .then(literal("connect").then(literal("mention").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.mentionRoleOnConnect = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Connect Mention " + toggleStrCaps(CONFIG.discord.mentionRoleOnConnect));
                return OK;
            }))))
            .then(literal("online").then(literal("mention").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.mentionRoleOnPlayerOnline = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Online Mention " + toggleStrCaps(CONFIG.discord.mentionRoleOnPlayerOnline));
                return OK;
            }))))
            .then(literal("disconnect").then(literal("mention").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.mentionRoleOnDisconnect = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Disconnect Mention " + toggleStrCaps(CONFIG.discord.mentionRoleOnDisconnect));
                return OK;
            }))))
            .then(literal("startQueue").then(literal("mention").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.mentionRoleOnStartQueue = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Start Queue Mention " + toggleStrCaps(CONFIG.discord.mentionRoleOnStartQueue));
                return OK;
            }))))
            .then(literal("death").then(literal("mention").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.mentionRoleOnDeath = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Death Mention " + toggleStrCaps(CONFIG.discord.mentionRoleOnDeath));
                return OK;
            }))))
            .then(literal("serverRestart").then(literal("mention").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.mentionRoleOnServerRestart = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Server Restart Mention " + toggleStrCaps(CONFIG.discord.mentionRoleOnServerRestart));
                return OK;
            }))))
            .then(literal("loginFailed").then(literal("mention").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.mentionRoleOnLoginFailed = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Login Failed Mention " + toggleStrCaps(CONFIG.discord.mentionRoleOnLoginFailed));
                return OK;
            }))))
            .then(literal("clientConnect").then(literal("mention").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.mentionOnClientConnected = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Client Connect Mention " + toggleStrCaps(CONFIG.discord.mentionOnClientConnected));
                return OK;
            }))))
            .then(literal("clientDisconnect").then(literal("mention").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.mentionOnClientDisconnected = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Client Disconnect Mention " + toggleStrCaps(CONFIG.discord.mentionOnClientDisconnected));
                return OK;
            }))))
            .then(literal("spectatorConnect").then(literal("mention").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.mentionOnSpectatorConnected = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Spectator Connect Mention " + toggleStrCaps(CONFIG.discord.mentionOnSpectatorConnected));
                return OK;
            }))))
            .then(literal("spectatorDisconnect").then(literal("mention").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.mentionOnSpectatorDisconnected = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Spectator Disconnect Mention " + toggleStrCaps(CONFIG.discord.mentionOnSpectatorDisconnected));
                return OK;
            }))))
            .then(literal("nonWhitelistedConnect").then(literal("mention").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.mentionOnNonWhitelistedClientConnected = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Non Whitelisted Connect Mention " + toggleStrCaps(CONFIG.discord.mentionOnNonWhitelistedClientConnected));
                return OK;
            }))));
    }

    @Override
    public void postPopulate(final Embed builder) {
        builder
            .primaryColor()
            .addField("Connect Mention", toggleStr(CONFIG.discord.mentionRoleOnConnect), false)
            .addField("Online Mention", toggleStr(CONFIG.discord.mentionRoleOnPlayerOnline), false)
            .addField("Disconnect Mention", toggleStr(CONFIG.discord.mentionRoleOnDisconnect), false)
            .addField("Start Queue Mention", toggleStr(CONFIG.discord.mentionRoleOnStartQueue), false)
            .addField("Death Mention", toggleStr(CONFIG.discord.mentionRoleOnDeath), false)
            .addField("Server Restart Mention", toggleStr(CONFIG.discord.mentionRoleOnServerRestart), false)
            .addField("Login Failed Mention", toggleStr(CONFIG.discord.mentionRoleOnLoginFailed), false)
            .addField("Client Connect Mention", toggleStr(CONFIG.discord.mentionOnClientConnected), false)
            .addField("Client Disconnect Mention", toggleStr(CONFIG.discord.mentionOnClientDisconnected), false)
            .addField("Spectator Connect Mention", toggleStr(CONFIG.discord.mentionOnSpectatorConnected), false)
            .addField("Spectator Disconnect Mention", toggleStr(CONFIG.discord.mentionOnSpectatorDisconnected), false)
            .addField("Non Whitelisted Connect Mention", toggleStr(CONFIG.discord.mentionOnNonWhitelistedClientConnected), false);
    }
}
