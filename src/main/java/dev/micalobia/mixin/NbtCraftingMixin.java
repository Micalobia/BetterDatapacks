package dev.micalobia.mixin;

import com.mojang.serialization.Codec;
import dev.micalobia.recipe.NbtRecipeCodecs;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

public class NbtCraftingMixin {
    @Mixin(ShapedRecipe.Serializer.class)
    public static class ShapedRecipeSerializerMixin {
        @Redirect(method = "method_55071", at = @At(value = "FIELD", target = "Lnet/minecraft/item/ItemStack;RECIPE_RESULT_CODEC:Lcom/mojang/serialization/Codec;"))
        private static Codec<ItemStack> changeResult() {
            return NbtRecipeCodecs.NBT_RESULT;
        }
    }

    @Mixin(ShapelessRecipe.Serializer.class)
    public static class ShapelessRecipeSerializerMixin {
        @Redirect(method = "method_53759", at = @At(value = "FIELD", target = "Lnet/minecraft/item/ItemStack;RECIPE_RESULT_CODEC:Lcom/mojang/serialization/Codec;"))
        private static Codec<ItemStack> changeResult() {
            return NbtRecipeCodecs.NBT_RESULT;
        }
    }

    @Mixin(SmithingTransformRecipe.Serializer.class)
    public static class SmithingTransformRecipeSerializerMixin {
        @Redirect(method = "method_53780", at = @At(value = "FIELD", target = "Lnet/minecraft/item/ItemStack;RECIPE_RESULT_CODEC:Lcom/mojang/serialization/Codec;"))
        private static Codec<ItemStack> changeResult() {
            return NbtRecipeCodecs.NBT_RESULT;
        }
    }
}
