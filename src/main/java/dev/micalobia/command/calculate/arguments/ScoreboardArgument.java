package dev.micalobia.command.calculate.arguments;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.micalobia.command.calculate.CalculateCommand;
import net.minecraft.command.argument.ScoreHolderArgumentType;
import net.minecraft.command.argument.ScoreboardObjectiveArgumentType;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;

public record ScoreboardArgument(String name) implements CalculateCommand.Input, CalculateCommand.Output {

    private static RequiredArgumentBuilder<ServerCommandSource, String> scoreboardObjective(String name) {
        return argument(name, ScoreboardObjectiveArgumentType.scoreboardObjective());
    }

    private static RequiredArgumentBuilder<ServerCommandSource, ScoreHolderArgumentType.ScoreHolder> scoreHolder(String name) {
        return argument(name, ScoreHolderArgumentType.scoreHolders()).suggests(ScoreHolderArgumentType.SUGGESTION_PROVIDER);
    }

    @Override
    public CalculateCommand.ArgType getOutputType(CommandContext<ServerCommandSource> ctx) {
        return CalculateCommand.ArgType.INT;
    }

    @Override
    public CalculateCommand.ArgType getInputType(CommandContext<ServerCommandSource> ctx) {
        return CalculateCommand.ArgType.INT;
    }

    @Override
    public void setInt(CommandContext<ServerCommandSource> ctx, int value) throws CommandSyntaxException {
        var player = ScoreHolderArgumentType.getScoreHolder(ctx, getTargetName());
        var objective = ScoreboardObjectiveArgumentType.getObjective(ctx, getObjectiveName());
        var scoreboard = ctx.getSource().getServer().getScoreboard();
        var score = new ScoreboardPlayerScore(scoreboard, objective, player);
        score.setScore(value);
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
        var player = ScoreHolderArgumentType.getScoreHolder(ctx, getTargetName());
        var objective = ScoreboardObjectiveArgumentType.getObjective(ctx, getObjectiveName());
        return ctx.getSource().getServer().getScoreboard().getPlayerScore(player, objective).getScore();
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

    private String getTargetName() {
        return name + "_target";
    }

    private String getObjectiveName() {
        return name + "_objective";
    }

    @Override
    public ArgumentBuilder<ServerCommandSource, ?> outputArgExecute(Command<ServerCommandSource> command) {
        return CommandManager.literal("scoreboard").then(scoreHolder(getTargetName()).then(scoreboardObjective(getObjectiveName()).executes(command)));
    }

    @Override
    public ArgumentBuilder<ServerCommandSource, ?> inputArgThen(ArgumentBuilder<ServerCommandSource, ?> then) {
        return CommandManager.literal("scoreboard").then(scoreHolder(getTargetName()).then(scoreboardObjective(getObjectiveName()).then(then)));
    }

    @Override
    public Text getSuccess(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        var name = ScoreHolderArgumentType.getScoreHolder(ctx, getTargetName());
        var objective = ScoreboardObjectiveArgumentType.getObjective(ctx, getObjectiveName());
        return Text.translatable("better_datapacks.commands.scoreboard.modify", objective, name);
    }
}
