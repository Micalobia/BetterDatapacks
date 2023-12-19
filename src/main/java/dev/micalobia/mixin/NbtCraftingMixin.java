package dev.micalobia.mixin;

import com.mojang.serialization.Codec;
import dev.micalobia.recipe.NbtRecipeCodecs;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.recipe.SmithingRecipe;
import net.minecraft.recipe.SmithingTransformRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

public class NbtCraftingMixin {
    @Mixin(ShapedRecipe.Serializer.RawShapedRecipe.class)
    public static class ShapedRecipeSerializerMixin {
        @Redirect(method = "method_53750", at = @At(value = "FIELD", target = "Lnet/minecraft/recipe/RecipeCodecs;CRAFTING_RESULT:Lcom/mojang/serialization/Codec;"))
        private static Codec<ItemStack> changeResult() {
            return NbtRecipeCodecs.NBT_RESULT;
        }
    }

    @Mixin(ShapelessRecipe.Serializer.class)
    public static class ShapelessRecipeSerializerMixin {
        @Redirect(method = "method_53759", at = @At(value = "FIELD", target = "Lnet/minecraft/recipe/RecipeCodecs;CRAFTING_RESULT:Lcom/mojang/serialization/Codec;"))
        private static Codec<ItemStack> changeResult() {
            return NbtRecipeCodecs.NBT_RESULT;
        }
    }

    @Mixin(SmithingTransformRecipe.Serializer.class)
    public static class SmithingTransformRecipeSerializerMixin {
        @Redirect(method = "method_53780", at = @At(value = "FIELD", target = "Lnet/minecraft/recipe/RecipeCodecs;CRAFTING_RESULT:Lcom/mojang/serialization/Codec;"))
        private static Codec<ItemStack> changeResult() {
            return NbtRecipeCodecs.NBT_RESULT;
        }
    }
}
