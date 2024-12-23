package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import static com.zenith.Shared.EXECUTOR;
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
            .then(argument("ip", StringArgumentType.string()))  // IP address argument
            .then(argument("port", IntegerArgumentType.integer(1, 65535)))  // Port argument, optional
            .executes(c -> {
                String ip = StringArgumentType.getString(c, "ip");
                Integer port = c.getArgument("port", Integer.class);
                
                if (port == null) {  // Check if port is null (not supplied)
                    port = 25565;  // Default to 25565 if port is null
                }

                EXECUTOR.execute(() -> Proxy.getInstance().connectAndCatchExceptions(ip, port));

                return 1; // Return value to indicate command execution
            });
    }
}
