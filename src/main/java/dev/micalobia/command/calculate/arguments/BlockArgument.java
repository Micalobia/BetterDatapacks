package dev.micalobia.command.calculate.arguments;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.block.Block;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.function.Function;

public record BlockArgument(String name) implements AbstractNbtArgument {
    private static final SimpleCommandExceptionType MUST_BE_BLOCK_ENTITY = new SimpleCommandExceptionType(Text.translatable("commands.data.block.invalid"));

    public <T extends Number, N extends NbtElement> void setValue(CommandContext<ServerCommandSource> ctx, T value, Function<T, N> factory) throws CommandSyntaxException {
        var pos = BlockPosArgumentType.getLoadedBlockPos(ctx, getPosName());
        var world = ctx.getSource().getWorld();
        var entity = world.getBlockEntity(pos);
        if (entity == null) throw MUST_BE_BLOCK_ENTITY.create();
        var path = NbtPathArgumentType.getNbtPath(ctx, getPathName());
        var nbt = entity.createNbt();
        path.put(nbt, factory.apply(value));
        entity.readNbt(nbt);
        entity.markDirty();
        var state = world.getBlockState(pos);
        world.updateListeners(pos, state, state, Block.NOTIFY_ALL);
    }

    public AbstractNbtNumber getValueType(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        var world = ctx.getSource().getWorld();
        var pos = BlockPosArgumentType.getLoadedBlockPos(ctx, world, getPosName());
        var entity = world.getBlockEntity(pos);
        if (entity == null) throw MUST_BE_BLOCK_ENTITY.create();
        var path = NbtPathArgumentType.getNbtPath(ctx, getPathName());
        var nbt = entity.createNbt();
        var value = AbstractNbtArgument.getNbt(path, nbt);
        if (!(value instanceof AbstractNbtNumber number)) throw AbstractNbtArgument.MUST_BE_NUMBER.create(path);
        return number;
    }

    @Override
    public String getName() {
        return name;
    }

    private String getPosName() {
        return name + "_pos";
    }

    private RequiredArgumentBuilder<ServerCommandSource, PosArgument> getPosArg() {
        return CommandManager.argument(getPosName(), BlockPosArgumentType.blockPos());
    }

    @Override
    public ArgumentBuilder<ServerCommandSource, ?> outputArgExecute(Command<ServerCommandSource> command) {
        return CommandManager.literal("block").then(getPosArg().then(getPathArg().then(getTypeArg().executes(command))));
    }

    @Override
    public ArgumentBuilder<ServerCommandSource, ?> inputArgThen(ArgumentBuilder<ServerCommandSource, ?> then) {
        return CommandManager.literal("block").then(getPosArg().then(getPathArg().then(then)));
    }

    @Override
    public Text getSuccess(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        var pos = BlockPosArgumentType.getLoadedBlockPos(ctx, getPosName());
        return Text.translatable("commands.data.block.modified", pos.getX(), pos.getY(), pos.getZ());
    }
}
