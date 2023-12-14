package dev.micalobia.command.calculate.arguments;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public record StorageArgument(String name) implements AbstractNbtArgument {
    public static final SuggestionProvider<ServerCommandSource> STORAGE_SUGGESTIONS = (context, builder) -> CommandSource.suggestIdentifiers(context.getSource().getServer().getDataCommandStorage().getIds(), builder);

    public <T extends Number, N extends NbtElement> void setValue(CommandContext<ServerCommandSource> ctx, T value, Function<T, N> factory) throws CommandSyntaxException {
        var id = IdentifierArgumentType.getIdentifier(ctx, getIdName());
        var path = NbtPathArgumentType.getNbtPath(ctx, getPathName());
        var storage = ctx.getSource().getServer().getDataCommandStorage();
        var nbt = storage.get(id);
        path.put(nbt, factory.apply(value));
        storage.set(id, nbt);
    }

    public AbstractNbtNumber getValueType(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        var id = IdentifierArgumentType.getIdentifier(ctx, getIdName());
        var path = NbtPathArgumentType.getNbtPath(ctx, getPathName());
        var storage = ctx.getSource().getServer().getDataCommandStorage();
        var nbt = storage.get(id);
        var value = AbstractNbtArgument.getNbt(path, nbt);
        if (!(value instanceof AbstractNbtNumber number))
            throw AbstractNbtArgument.MUST_BE_NUMBER.create(path);
        return number;
    }

    @Override
    public String getName() {
        return name;
    }

    private String getIdName() {
        return name + "_id";
    }

    private RequiredArgumentBuilder<ServerCommandSource, Identifier> getIdArg() {
        return CommandManager.argument(getIdName(), IdentifierArgumentType.identifier()).suggests(StorageArgument.STORAGE_SUGGESTIONS);
    }

    @Override
    public ArgumentBuilder<ServerCommandSource, ?> outputArgExecute(Command<ServerCommandSource> command) {
        return CommandManager.literal("storage").then(getIdArg().then(getPathArg().then(getTypeArg().executes(command))));
    }

    @Override
    public ArgumentBuilder<ServerCommandSource, ?> inputArgThen(ArgumentBuilder<ServerCommandSource, ?> then) {
        return CommandManager.literal("storage").then(getIdArg().then(getPathArg().then(then)));
    }

    @Override
    public Text getSuccess(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        var id = IdentifierArgumentType.getIdentifier(ctx, getIdName());
        return Text.translatable("commands.data.storage.modified", id);
    }
}
