package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.command.brigadier.CustomStringArgumentType.getString;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;

public class TransferCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.simple(
            "transfer",
            CommandCategory.MANAGE,
            "Transfers connected players to a destination MC server"
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("transfer")
            // todo: add permission check
            // todo: argument for transferring a selected player
            .then(argument("address", wordWithChars()).then(argument("port", integer(1, 65535)).executes(ctx -> {
                String address = getString(ctx, "address");
                int port = getInteger(ctx, "port");
                var connections = Proxy.getInstance().getActiveConnections().getArray();
                if (connections.length == 0) {
                    ctx.getSource().getEmbed()
                        .title("Error")
                        .errorColor()
                        .description("No players connected");
                    return OK;
                }
                for (int i = 0; i < connections.length; i++) {
                    var connection = connections[i];
                    connection.transfer(address, port);
                }
                ctx.getSource().getEmbed()
                    .title("Transferred")
                    .primaryColor()
                    .addField("Address", address, false)
                    .addField("Port", port, false);
                return OK;
            })));
    }
}
