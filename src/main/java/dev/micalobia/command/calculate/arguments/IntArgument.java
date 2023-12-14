package dev.micalobia.command.calculate.arguments;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.micalobia.command.calculate.CalculateCommand;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public record IntArgument(String name) implements CalculateCommand.Input {
    @Override
    public CalculateCommand.ArgType getInputType(CommandContext<ServerCommandSource> ctx) {
        return CalculateCommand.ArgType.INT;
    }

    @Override
    public int getInt(CommandContext<ServerCommandSource> ctx) {
        return ctx.getArgument(name, Integer.class);
    }

    @Override
    public long getLong(CommandContext<ServerCommandSource> ctx) {
        return getInt(ctx);
    }

    @Override
    public float getFloat(CommandContext<ServerCommandSource> ctx) {
        return getInt(ctx);
    }

    @Override
    public double getDouble(CommandContext<ServerCommandSource> ctx) {
        return getInt(ctx);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ArgumentBuilder<ServerCommandSource, ?> inputArgThen(ArgumentBuilder<ServerCommandSource, ?> then) {
        return CommandManager.literal("int").then(CommandManager.argument(name, IntegerArgumentType.integer()).then(then));
    }
}
