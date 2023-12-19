package dev.micalobia.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.dynamic.Codecs;

import java.util.Optional;

public enum NbtRecipeCodecs {
    ;
    private static final Codec<Item> RESULT_ITEM = Codecs.validate(Registries.ITEM.getCodec(), item -> item == Items.AIR ? DataResult.error(() -> "Crafting result must not be minecraft:air") : DataResult.success(item));
    public static final Codec<ItemStack> NBT_RESULT = RecordCodecBuilder.create(instance -> instance.group(
            RESULT_ITEM.fieldOf("item").forGetter(ItemStack::getItem),
            Codecs.createStrictOptionalFieldCodec(Codecs.POSITIVE_INT, "count", 1).forGetter(ItemStack::getCount),
            NbtCompound.CODEC.optionalFieldOf("tag").forGetter(stack -> Optional.ofNullable(stack.getNbt()))
    ).apply(instance, NbtRecipeCodecs::create));

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static ItemStack create(Item item, int count, Optional<NbtCompound> nbt) {
        var stack = new ItemStack(item, count);
        nbt.ifPresent(stack::setNbt);
        return stack;
    }
}
