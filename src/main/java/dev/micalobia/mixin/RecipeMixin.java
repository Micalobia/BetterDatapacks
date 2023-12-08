package dev.micalobia.mixin;

import com.google.gson.JsonObject;
import dev.micalobia.BetterDatapacks;
import dev.micalobia.PotionRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipeManager.class)
public class RecipeMixin {
    @Inject(method = "deserialize", at = @At(value = "HEAD"), cancellable = true)
    private static void interceptRecipes(Identifier id, JsonObject json, CallbackInfoReturnable<RecipeEntry<?>> cir) {
        var type = JsonHelper.getString(json, "type");
        if (type.equals(BetterDatapacks.idString("empty"))) {
            cir.setReturnValue(new RecipeEntry<>(id, emptyRecipe()));
        } else if (type.equals(BetterDatapacks.idString("potion"))) {
            cir.setReturnValue(new RecipeEntry<>(id, emptyRecipe()));
            PotionRecipe.register(id, json);
        }
    }

    @Unique
    private static ShapelessRecipe emptyRecipe() {
        return new ShapelessRecipe("empty", CraftingRecipeCategory.MISC, ItemStack.EMPTY, DefaultedList.of());
    }
}
