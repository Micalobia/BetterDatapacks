package dev.micalobia.command.calculate.arguments;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.micalobia.command.calculate.CalculateCommand;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public record DoubleArgument(String name) implements CalculateCommand.Input {
    @Override
    public CalculateCommand.ArgType getInputType(CommandContext<ServerCommandSource> ctx) {
        return CalculateCommand.ArgType.DOUBLE;
    }

    @Override
    public int getInt(CommandContext<ServerCommandSource> ctx) {
        return (int) getDouble(ctx);
    }

    @Override
    public long getLong(CommandContext<ServerCommandSource> ctx) {
        return (long) getDouble(ctx);
    }

    @Override
    public float getFloat(CommandContext<ServerCommandSource> ctx) {
        return (float) getDouble(ctx);
    }

    @Override
    public double getDouble(CommandContext<ServerCommandSource> ctx) {
        return ctx.getArgument(name, Double.class);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ArgumentBuilder<ServerCommandSource, ?> inputArgThen(ArgumentBuilder<ServerCommandSource, ?> then) {
        return CommandManager.literal("double").then(CommandManager.argument(name, DoubleArgumentType.doubleArg()).then(then));
    }


}
