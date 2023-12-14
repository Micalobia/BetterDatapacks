package dev.micalobia.command.calculate.arguments;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.micalobia.command.CommandUtility;
import dev.micalobia.command.calculate.CalculateCommand;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.entity.boss.CommandBossBar;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;

import static net.minecraft.server.command.CommandManager.argument;

public record BossbarArgument(String name) implements CalculateCommand.Input, CalculateCommand.Output {
    private static final SuggestionProvider<ServerCommandSource> BOSSBAR_SUGGESTIONS = (context, builder) -> CommandSource.suggestIdentifiers(context.getSource().getServer().getBossBarManager().getIds(), builder);

    public static RequiredArgumentBuilder<ServerCommandSource, Identifier> bossbar(String name) {
        return argument(name, IdentifierArgumentType.identifier()).suggests(BOSSBAR_SUGGESTIONS);
    }

    public static RequiredArgumentBuilder<ServerCommandSource, String> bossbarMode(String name) {
        return CommandUtility.generateLiteralEnumArgument(name, BossbarMode::values);
    }

    @Override
    public CalculateCommand.ArgType getOutputType(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        return CalculateCommand.ArgType.INT;
    }

    @Override
    public CalculateCommand.ArgType getInputType(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        return CalculateCommand.ArgType.INT;
    }

    private CommandBossBar getBar(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        var id = IdentifierArgumentType.getIdentifier(ctx, getIdName());
        var bar = ctx.getSource().getServer().getBossBarManager().get(id);
        if (bar == null)
            throw new SimpleCommandExceptionType(Text.translatable("commands.bossbar.unknown", id)).create();
        return bar;
    }

    @Override
    public void setInt(CommandContext<ServerCommandSource> ctx, int value) throws CommandSyntaxException {
        var mode = CommandUtility.getLiteralEnumArgument(ctx, getModeName(), BossbarMode::values);
        var bar = getBar(ctx);
        switch (mode) {
            case VALUE -> bar.setValue(value);
            case MAX -> bar.setMaxValue(value);
        }
    }

    @Override
    public void setLong(CommandContext<ServerCommandSource> ctx, long value) throws CommandSyntaxException {
        setInt(ctx, (int) value);
    }

    @Override
    public void setFloat(CommandContext<ServerCommandSource> ctx, float value) throws CommandSyntaxException {
        setInt(ctx, (int) value);
    }

    @Override
    public void setDouble(CommandContext<ServerCommandSource> ctx, double value) throws CommandSyntaxException {
        setInt(ctx, (int) value);
    }

    @Override
    public int getInt(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        var mode = CommandUtility.getLiteralEnumArgument(ctx, getModeName(), BossbarMode::values);
        var bar = getBar(ctx);
        return switch (mode) {
            case VALUE -> bar.getValue();
            case MAX -> bar.getMaxValue();
        };
    }

    @Override
    public long getLong(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        return getInt(ctx);
    }

    @Override
    public float getFloat(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        return getInt(ctx);
    }

    @Override
    public double getDouble(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        return getInt(ctx);
    }

    @Override
    public String getName() {
        return name;
    }

    private String getIdName() {
        return name + "_id";
    }

    private String getModeName() {
        return name + "_mode";
    }

    @Override
    public ArgumentBuilder<ServerCommandSource, ?> outputArgExecute(Command<ServerCommandSource> command) {
        return CommandManager.literal("bossbar")
                .then(bossbar(getIdName())
                        .then(bossbarMode(getModeName())
                                .executes(command)));
    }

    @Override
    public ArgumentBuilder<ServerCommandSource, ?> inputArgThen(ArgumentBuilder<ServerCommandSource, ?> then) {
        return CommandManager.literal("bossbar")
                .then(bossbar(getIdName())
                        .then(bossbarMode(getModeName())
                                .then(then)));
    }

    @Override
    public Text getSuccess(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        var id = IdentifierArgumentType.getIdentifier(ctx, getIdName());
        var mode = CommandUtility.getLiteralEnumArgument(ctx, getModeName(), BossbarMode::values).asString();
        return Text.translatable("better_datapacks.commands.bossbar.modify", mode, id);
    }

    public enum BossbarMode implements StringIdentifiable {
        VALUE("value"),
        MAX("max");

        private final String name;

        BossbarMode(String name) {
            this.name = name;
        }

        @Override
        public String asString() {
            return name;
        }
    }
}
