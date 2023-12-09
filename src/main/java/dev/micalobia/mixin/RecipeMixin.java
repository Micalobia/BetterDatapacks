package dev.micalobia.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.micalobia.BetterDatapacks;
import dev.micalobia.PotionRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(RecipeManager.class)
public class RecipeMixin {
    @Inject(method = "deserialize", at = @At("HEAD"), cancellable = true)
    private static void interceptRecipes(Identifier id, JsonObject json, CallbackInfoReturnable<RecipeEntry<?>> cir) {
        var type = JsonHelper.getString(json, "type");
        switch (type) {
            case "better_datapacks:potion":
                BetterDatapacks.LOGGER.info(id.toString());
                PotionRecipe.PotionType.register(id, json);
                cancel(cir, id);
                break;
            case "better_datapacks:potion_item":
                PotionRecipe.ItemType.register(id, json);
                cancel(cir, id);
                break;
            case "better_datapacks:empty":
                cancel(cir, id);
                break;
        }
    }

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/util/profiler/Profiler;)V", at = @At("HEAD"))
    private void cacheBaseRecipes(Map<Identifier, JsonElement> map, ResourceManager resourceManager, Profiler profiler, CallbackInfo ci) {
        PotionRecipe.createCache();
    }

    @Unique
    private static ShapelessRecipe emptyRecipe() {
        return new ShapelessRecipe("empty", CraftingRecipeCategory.MISC, ItemStack.EMPTY, DefaultedList.of());
    }

    @Unique
    private static void cancel(CallbackInfoReturnable<RecipeEntry<?>> cir, Identifier id) {
        cir.setReturnValue(new RecipeEntry<>(id, emptyRecipe()));
    }
}
