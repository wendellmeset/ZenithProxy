package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;

import static com.mojang.brigadier.arguments.FloatArgumentType.floatArg;
import static com.mojang.brigadier.arguments.FloatArgumentType.getFloat;
import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.PATHING;
import static java.util.Arrays.asList;

public class RotateCommand extends Command {
    private static final int MOVE_PRIORITY = 1000000;

    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "rotate",
            CommandCategory.MODULE,
            """
            Rotates the bot in-game.
            
            Note that many other modules can change the player's rotation after this command is executed.
            """,
            asList(
                "<yaw> <pitch>",
                "yaw <yaw>",
                "pitch <pitch>"
            )
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("rotate")
            .then(argument("yaw", floatArg(-180, 180)).then(argument("pitch", floatArg(-90, 90)).executes(c -> {
                float yaw = getFloat(c, "yaw");
                float pitch = getFloat(c, "pitch");
                if (!Proxy.getInstance().isConnected()) {
                    c.getSource().getEmbed()
                        .title("Error")
                        .errorColor()
                        .description("Not connected to a server");
                    return OK;
                }
                if (Proxy.getInstance().hasActivePlayer()) {
                    c.getSource().getEmbed()
                        .title("Error")
                        .errorColor()
                        .description("Cannot rotate while player is controlling");
                    return OK;
                }
                PATHING.rotate(yaw, pitch, MOVE_PRIORITY);
                c.getSource().getEmbed()
                    .title("Rotated")
                    .successColor();
                return OK;
            })))
            .then(literal("yaw").then(argument("yaw", floatArg(-180, 180)).executes(c -> {
                float yaw = getFloat(c, "yaw");
                if (!Proxy.getInstance().isConnected()) {
                    c.getSource().getEmbed()
                        .title("Error")
                        .errorColor()
                        .description("Not connected to a server");
                    return OK;
                }
                if (Proxy.getInstance().hasActivePlayer()) {
                    c.getSource().getEmbed()
                        .title("Error")
                        .errorColor()
                        .description("Cannot rotate while player is controlling");
                    return OK;
                }
                PATHING.rotate(yaw, CACHE.getPlayerCache().getPitch(), MOVE_PRIORITY);
                c.getSource().getEmbed()
                    .title("Rotated")
                    .successColor();
                return OK;
            })))
            .then(literal("pitch").then(argument("pitch", floatArg(-90, 90)).executes(c -> {
                float pitch = getFloat(c, "pitch");
                if (!Proxy.getInstance().isConnected()) {
                    c.getSource().getEmbed()
                        .title("Error")
                        .errorColor()
                        .description("Not connected to a server");
                    return OK;
                }
                if (Proxy.getInstance().hasActivePlayer()) {
                    c.getSource().getEmbed()
                        .title("Error")
                        .errorColor()
                        .description("Cannot rotate while player is controlling");
                    return OK;
                }
                PATHING.rotate(CACHE.getPlayerCache().getYaw(), pitch, MOVE_PRIORITY);
                c.getSource().getEmbed()
                    .title("Rotated")
                    .successColor();
                return OK;
            })));
    }
}
