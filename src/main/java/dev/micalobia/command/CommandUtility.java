package dev.micalobia.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;

import java.util.function.Supplier;

import static net.minecraft.server.command.CommandManager.argument;

public class CommandUtility {
    public static <E extends Enum<E> & StringIdentifiable> RequiredArgumentBuilder<ServerCommandSource, String> generateLiteralEnumArgument(String name, Supplier<E[]> valuesSupplier) {
        return argument(name, StringArgumentType.word()).suggests(literalEnumSuggestions(valuesSupplier));
    }

    private static <E extends Enum<E> & StringIdentifiable> SuggestionProvider<ServerCommandSource> literalEnumSuggestions(Supplier<E[]> valuesSupplier) {
        return ((context, builder) -> {
            for (var value : valuesSupplier.get()) {
                builder.suggest(value.asString());
            }
            return builder.buildFuture();
        });
    }

    public static <S, E extends Enum<E> & StringIdentifiable> E getLiteralEnumArgument(CommandContext<S> context, String name, Supplier<E[]> valuesSupplier) throws CommandSyntaxException {
        var arg = context.getArgument(name, String.class);
        for (var value : valuesSupplier.get()) {
            if (value.asString().equals(arg)) return value;
        }
        throw new SimpleCommandExceptionType(Text.translatable("argument.enum.invalid", arg)).create();
    }


}
