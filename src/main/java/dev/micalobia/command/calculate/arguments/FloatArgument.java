package dev.micalobia.command.calculate.arguments;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.micalobia.command.calculate.CalculateCommand;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public record FloatArgument(String name) implements CalculateCommand.Input {
    @Override
    public CalculateCommand.ArgType getInputType(CommandContext<ServerCommandSource> ctx) {
        return CalculateCommand.ArgType.FLOAT;
    }

    @Override
    public int getInt(CommandContext<ServerCommandSource> ctx) {
        return (int) getFloat(ctx);
    }

    @Override
    public long getLong(CommandContext<ServerCommandSource> ctx) {
        return (long) getFloat(ctx);
    }

    @Override
    public float getFloat(CommandContext<ServerCommandSource> ctx) {
        return ctx.getArgument(name, Float.class);
    }

    @Override
    public double getDouble(CommandContext<ServerCommandSource> ctx) {
        return getFloat(ctx);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ArgumentBuilder<ServerCommandSource, ?> inputArgThen(ArgumentBuilder<ServerCommandSource, ?> then) {
        return CommandManager.literal("float").then(CommandManager.argument(name, FloatArgumentType.floatArg()).then(then));
    }
}
