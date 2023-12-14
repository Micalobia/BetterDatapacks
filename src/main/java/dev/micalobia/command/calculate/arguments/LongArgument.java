package dev.micalobia.command.calculate.arguments;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.micalobia.command.calculate.CalculateCommand;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public record LongArgument(String name) implements CalculateCommand.Input {
    @Override
    public CalculateCommand.ArgType getInputType(CommandContext<ServerCommandSource> ctx) {
        return CalculateCommand.ArgType.LONG;
    }

    @Override
    public int getInt(CommandContext<ServerCommandSource> ctx) {
        return (int)getLong(ctx);
    }

    @Override
    public long getLong(CommandContext<ServerCommandSource> ctx) {
        return ctx.getArgument(name, Long.class);
    }

    @Override
    public float getFloat(CommandContext<ServerCommandSource> ctx) {
        return getLong(ctx);
    }

    @Override
    public double getDouble(CommandContext<ServerCommandSource> ctx) {
        return getLong(ctx);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ArgumentBuilder<ServerCommandSource, ?> inputArgThen(ArgumentBuilder<ServerCommandSource, ?> then) {
        return CommandManager.literal("long").then(CommandManager.argument(name, LongArgumentType.longArg()).then(then));
    }
}
