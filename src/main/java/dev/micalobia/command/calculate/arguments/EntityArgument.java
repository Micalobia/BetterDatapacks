package dev.micalobia.command.calculate.arguments;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.function.Function;

public record EntityArgument(String name) implements AbstractNbtArgument {
    private static final SimpleCommandExceptionType NO_PLAYER = new SimpleCommandExceptionType(Text.translatable("commands.data.entity.invalid"));

    @Override
    public String getName() {
        return name;
    }

    private String getEntityName() {
        return name + "_entity";
    }

    private RequiredArgumentBuilder<ServerCommandSource, EntitySelector> getEntityArg() {
        return CommandManager.argument(getEntityName(), EntityArgumentType.entity());
    }

    @Override
    public ArgumentBuilder<ServerCommandSource, ?> outputArgExecute(Command<ServerCommandSource> command) {
        return CommandManager.literal("entity").then(getEntityArg().then(getPathArg().then(getTypeArg().executes(command))));
    }

    @Override
    public ArgumentBuilder<ServerCommandSource, ?> inputArgThen(ArgumentBuilder<ServerCommandSource, ?> then) {
        return CommandManager.literal("entity").then(getEntityArg().then(getPathArg().then(then)));
    }

    @Override
    public AbstractNbtNumber getValueType(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        var entity = EntityArgumentType.getEntity(ctx, getEntityName());
        var path = NbtPathArgumentType.getNbtPath(ctx, getPathName());
        var nbt = entity.writeNbt(new NbtCompound());
        var value = AbstractNbtArgument.getNbt(path, nbt);
        if (!(value instanceof AbstractNbtNumber number)) throw AbstractNbtArgument.MUST_BE_NUMBER.create(path);
        return number;
    }

    @Override
    public <T extends Number, N extends NbtElement> void setValue(CommandContext<ServerCommandSource> ctx, T value, Function<T, N> factory) throws CommandSyntaxException {
        var entity = EntityArgumentType.getEntity(ctx, getEntityName());
        if (entity instanceof PlayerEntity) throw NO_PLAYER.create();
        var path = NbtPathArgumentType.getNbtPath(ctx, getPathName());
        var nbt = entity.writeNbt(new NbtCompound());
        path.put(nbt, factory.apply(value));
        entity.readNbt(nbt);
    }

    @Override
    public Text getSuccess(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        var entity = EntityArgumentType.getEntity(ctx, getEntityName());
        return Text.translatable("commands.data.entity.modified", entity.getDisplayName());
    }
}
