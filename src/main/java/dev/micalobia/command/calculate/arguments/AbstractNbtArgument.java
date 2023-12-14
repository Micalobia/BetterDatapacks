package dev.micalobia.command.calculate.arguments;

import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.micalobia.command.CommandUtility;
import dev.micalobia.command.calculate.CalculateCommand;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.nbt.*;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.function.Function;

public interface AbstractNbtArgument extends CalculateCommand.Input, CalculateCommand.Output {
    DynamicCommandExceptionType MUST_BE_NUMBER = new DynamicCommandExceptionType(x -> Text.translatable("commands.data.get.invalid", x));
    SimpleCommandExceptionType MUST_BE_SINGLE_NBT = new SimpleCommandExceptionType(Text.translatable("commands.data.get.multiple"));

    static NbtElement getNbt(NbtPathArgumentType.NbtPath path, NbtCompound nbt) throws CommandSyntaxException {
        var list = path.get(nbt);
        var iterator = list.iterator();
        var next = iterator.next();
        if (iterator.hasNext())
            throw MUST_BE_SINGLE_NBT.create();
        return next;
    }

    @Override
    default CalculateCommand.ArgType getOutputType(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        return CommandUtility.getLiteralEnumArgument(ctx, getTypeName(), CalculateCommand.ArgType::values);
    }

    @Override
    default CalculateCommand.ArgType getInputType(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        var type = getValueType(ctx).getType();
        return switch (type) {
            case NbtCompound.BYTE_TYPE, NbtCompound.SHORT_TYPE, NbtCompound.INT_TYPE -> CalculateCommand.ArgType.INT;
            case NbtCompound.LONG_TYPE -> CalculateCommand.ArgType.LONG;
            case NbtCompound.FLOAT_TYPE -> CalculateCommand.ArgType.FLOAT;
            case NbtCompound.DOUBLE_TYPE -> CalculateCommand.ArgType.DOUBLE;
            default ->
                    throw new AssertionError("Shouldn't be possible to get here, since we checked if it was a number beforehand");
        };
    }

    @Override
    default int getInt(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        return getValue(ctx, AbstractNbtNumber::intValue);
    }

    @Override
    default long getLong(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        return getValue(ctx, AbstractNbtNumber::longValue);
    }

    @Override
    default float getFloat(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        return getValue(ctx, AbstractNbtNumber::floatValue);
    }

    @Override
    default double getDouble(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        return getValue(ctx, AbstractNbtNumber::doubleValue);
    }

    @Override
    default void setInt(CommandContext<ServerCommandSource> ctx, int value) throws CommandSyntaxException {
        setValue(ctx, value, NbtInt::of);
    }

    @Override
    default void setLong(CommandContext<ServerCommandSource> ctx, long value) throws CommandSyntaxException {
        setValue(ctx, value, NbtLong::of);
    }

    @Override
    default void setFloat(CommandContext<ServerCommandSource> ctx, float value) throws CommandSyntaxException {
        setValue(ctx, value, NbtFloat::of);
    }

    @Override
    default void setDouble(CommandContext<ServerCommandSource> ctx, double value) throws CommandSyntaxException {
        setValue(ctx, value, NbtDouble::of);
    }

    default String getTypeName() {
        return getName() + "_type";
    }

    default String getPathName() {
        return getName() + "_path";
    }

    default RequiredArgumentBuilder<ServerCommandSource, String> getTypeArg() {
        return CommandUtility.generateLiteralEnumArgument(getTypeName(), CalculateCommand.ArgType::values);
    }

    default RequiredArgumentBuilder<ServerCommandSource, NbtPathArgumentType.NbtPath> getPathArg() {
        return CommandManager.argument(getPathName(), NbtPathArgumentType.nbtPath());
    }

    default <T extends Number> T getValue(CommandContext<ServerCommandSource> ctx, Function<AbstractNbtNumber, T> getter) throws CommandSyntaxException {
        return getter.apply(getValueType(ctx));
    }

    AbstractNbtNumber getValueType(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException;

    <T extends Number, N extends NbtElement> void setValue(CommandContext<ServerCommandSource> ctx, T value, Function<T, N> factory) throws CommandSyntaxException;
}
