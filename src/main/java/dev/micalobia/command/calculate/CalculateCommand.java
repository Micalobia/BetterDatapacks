package dev.micalobia.command.calculate;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.micalobia.BetterDatapacks;
import dev.micalobia.command.calculate.arguments.*;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;

import java.util.List;
import java.util.function.Function;

public class CalculateCommand {
    private static final List<Function<String, Input>> INPUT_GENERATORS = List.of(
            BlockArgument::new,
            BossbarArgument::new,
            DoubleArgument::new,
            EntityArgument::new,
            FloatArgument::new,
            IntArgument::new,
            LongArgument::new,
            ScoreboardArgument::new,
            StorageArgument::new
    );

    private static final List<Function<String, Output>> OUTPUT_GENERATORS = List.of(
            BlockArgument::new,
            BossbarArgument::new,
            EntityArgument::new,
            ScoreboardArgument::new,
            StorageArgument::new
    );

    private static final List<Input> INPUTS = INPUT_GENERATORS.stream().map(x -> x.apply("input")).toList();
    private static final List<Input> LEFT_INPUTS = INPUT_GENERATORS.stream().map(x -> x.apply("left")).toList();
    private static final List<Input> RIGHT_INPUTS = INPUT_GENERATORS.stream().map(x -> x.apply("right")).toList();
    private static final List<Output> OUTPUTS = OUTPUT_GENERATORS.stream().map(x -> x.apply("output")).toList();

    private static final List<UnaryOperation> UNARY_OPERATIONS = List.of(
            new UnaryOperation("abs", MathUtility::abs),
            new UnaryOperation("acos", MathUtility::acos),
            new UnaryOperation("asin", MathUtility::asin),
            new UnaryOperation("atan", MathUtility::atan),
            new UnaryOperation("ceil", MathUtility::ceil),
            new UnaryOperation("cos", MathUtility::cos),
            new UnaryOperation("cosh", MathUtility::cosh),
            new UnaryOperation("floor", MathUtility::floor),
            new UnaryOperation("sin", MathUtility::sin),
            new UnaryOperation("sinh", MathUtility::sinh),
            new UnaryOperation("sqrt", MathUtility::sqrt),
            new UnaryOperation("tan", MathUtility::tan),
            new UnaryOperation("tanh", MathUtility::tanh)
    );

    private static final List<BinaryOperation> BINARY_OPERATIONS = List.of(
            new BinaryOperation("add", MathUtility::add),
            new BinaryOperation("atan2", MathUtility::atan2),
            new BinaryOperation("divide", MathUtility::divide),
            new BinaryOperation("max", MathUtility::max),
            new BinaryOperation("min", MathUtility::min),
            new BinaryOperation("modulus", MathUtility::modulus),
            new BinaryOperation("multiply", MathUtility::multiply),
            new BinaryOperation("power", MathUtility::power),
            new BinaryOperation("subtract", MathUtility::subtract)
    );

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        var root = CommandManager.literal("calculate").requires(source -> source.hasPermissionLevel(2));
        UNARY_OPERATIONS.forEach(op -> op.createCommands(root));
        BINARY_OPERATIONS.forEach(op -> op.createCommands(root));
        dispatcher.register(root);
    }

    public enum ArgType implements StringIdentifiable {
        INT("int"), LONG("long"), FLOAT("float"), DOUBLE("double");

        private final String name;

        ArgType(String name) {
            this.name = name;
        }


        @Override
        public String asString() {
            return name;
        }
    }

    public interface Input {

        ArgType getInputType(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException;

        int getInt(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException;

        long getLong(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException;

        float getFloat(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException;

        double getDouble(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException;

        default Number getNumber(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
            return switch (getInputType(ctx)) {
                case INT -> getInt(ctx);
                case LONG -> getLong(ctx);
                case FLOAT -> getFloat(ctx);
                case DOUBLE -> getDouble(ctx);
            };
        }

        String getName();

        ArgumentBuilder<ServerCommandSource, ?> inputArgThen(ArgumentBuilder<ServerCommandSource, ?> then);
    }

    public interface Output {
        ArgType getOutputType(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException;

        void setInt(CommandContext<ServerCommandSource> ctx, int value) throws CommandSyntaxException;

        void setLong(CommandContext<ServerCommandSource> ctx, long value) throws CommandSyntaxException;

        void setFloat(CommandContext<ServerCommandSource> ctx, float value) throws CommandSyntaxException;

        void setDouble(CommandContext<ServerCommandSource> ctx, double value) throws CommandSyntaxException;

        default void setNumber(CommandContext<ServerCommandSource> ctx, Number value) throws CommandSyntaxException {
            switch (getOutputType(ctx)) {
                case INT -> setInt(ctx, value.intValue());
                case LONG -> setLong(ctx, value.longValue());
                case FLOAT -> setFloat(ctx, value.floatValue());
                case DOUBLE -> setDouble(ctx, value.doubleValue());
            }
        }

        String getName();

        ArgumentBuilder<ServerCommandSource, ?> outputArgExecute(Command<ServerCommandSource> command);

        Text getSuccess(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException;
    }

    public record UnaryOperation(String name, UnaryOperation.Operation action) {
        void createCommand(ArgumentBuilder<ServerCommandSource, ?> root, Input input, Output output) {
            root.then(CommandManager.literal(name).then(input.inputArgThen(output.outputArgExecute(ctx -> {
                try {
                    output.setNumber(ctx, action.apply(input.getNumber(ctx)));
                } catch (Exception err) {
                    BetterDatapacks.LOGGER.info(err.getMessage());
                    return 0;
                }
                ctx.getSource().sendFeedback(() -> {
                    try {
                        return output.getSuccess(ctx);
                    } catch (CommandSyntaxException e) {
                        return Text.literal(e.getMessage());
                    }
                }, false);
                return 1;
            }))));
        }

        void createCommands(ArgumentBuilder<ServerCommandSource, ?> root) {
            for (var input : CalculateCommand.INPUTS) {
                for (var output : CalculateCommand.OUTPUTS) {
                    createCommand(root, input, output);
                }
            }
        }


        @FunctionalInterface
        public interface Operation {
            Number apply(Number input);
        }
    }

    public record BinaryOperation(String name, BinaryOperation.Operation action) {


        private void createCommand(ArgumentBuilder<ServerCommandSource, ?> root, Input left, Input right, Output output) {
            root.then(CommandManager.literal(name).then(left.inputArgThen(right.inputArgThen(output.outputArgExecute(ctx -> {
                try {
                    output.setNumber(ctx, action.apply(left.getNumber(ctx), right.getNumber(ctx)));
                } catch (Exception err) {
                    BetterDatapacks.LOGGER.info(err.getMessage());
                    return 0;
                }
                ctx.getSource().sendFeedback(() -> {
                    try {
                        return output.getSuccess(ctx);
                    } catch (CommandSyntaxException e) {
                        return Text.literal(e.getMessage());
                    }
                }, false);
                return 1;
            })))));
        }

        void createCommands(ArgumentBuilder<ServerCommandSource, ?> root) {
            for (var left : CalculateCommand.LEFT_INPUTS) {
                for (var right : CalculateCommand.RIGHT_INPUTS) {
                    for (var output : CalculateCommand.OUTPUTS) {
                        createCommand(root, left, right, output);
                    }
                }
            }
        }

        @FunctionalInterface
        public interface Operation {
            Number apply(Number left, Number right);
        }
    }
}
