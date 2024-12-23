package com.zenith.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;

import static com.zenith.Shared.EXECUTOR;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;
import static java.util.Arrays.asList;

public class ConnectCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.simpleAliases(
            "connect",
            CommandCategory.CORE,
            "Connects ZenithProxy to the destination MC server",
            asList("c")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("connect")
            .then(argument("serverIP", string()) // Required server IP argument
                .then(argument("port", integer(1, 65535)) // Optional port argument
                    .executes(c -> {
                        String serverIP = c.getArgument("serverIP", String.class);
                        int port = c.getArgument("port", Integer.class); // Get the port argument

                        // Execute the connection logic with serverIP and port
                        EXECUTOR.execute(() -> {
                            Proxy.getInstance().connectAndCatchExceptions(serverIP, port);
                        });
                        return 1;
                    })))
            .then(argument("serverIP", string()) // If no port is provided, default to 25565
                .executes(c -> {
                    String serverIP = c.getArgument("serverIP", String.class);
                    int port = 25565; // Default to 25565 if no port argument is provided

                    // Execute the connection logic with serverIP and default port
                    EXECUTOR.execute(() -> {
                        Proxy.getInstance().connectAndCatchExceptions(serverIP, port);
                    });
                    return 1;
                }));
    }
}
