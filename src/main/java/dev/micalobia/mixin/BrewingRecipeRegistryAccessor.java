package dev.micalobia.mixin;

import net.minecraft.item.Item;
import net.minecraft.potion.Potion;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.recipe.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(BrewingRecipeRegistry.class)
public interface BrewingRecipeRegistryAccessor {
    @Accessor("POTION_TYPES")
    static List<Ingredient> getPotionTypes() {
        throw new AssertionError();
    }


    @Invoker("registerPotionRecipe")
    static void registerPotionRecipe(Potion input, Item item, Potion output) {
    }

    @Invoker("registerItemRecipe")
    static void registerItemRecipe(Item input, Item item, Item output) {
    }
}
