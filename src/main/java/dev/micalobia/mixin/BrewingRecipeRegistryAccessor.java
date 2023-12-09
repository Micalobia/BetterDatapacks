package dev.micalobia.mixin;

import net.minecraft.item.Item;
import net.minecraft.potion.Potion;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.recipe.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(BrewingRecipeRegistry.class)
public interface BrewingRecipeRegistryAccessor {
    @Accessor("POTION_TYPES")
    static List<Ingredient> getPotionTypes() {
        throw new AssertionError();
    }

    @Accessor("ITEM_RECIPES")
    static List<BrewingRecipeRegistry.Recipe<Item>> getItemRecipes() {
        throw new AssertionError();
    }

    @Accessor("POTION_RECIPES")
    static List<BrewingRecipeRegistry.Recipe<Potion>> getPotionRecipes() {
        throw new AssertionError();
    }
}
