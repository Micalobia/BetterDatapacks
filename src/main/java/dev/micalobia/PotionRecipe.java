package dev.micalobia;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.micalobia.mixin.BrewingRecipeRegistryAccessor;
import dev.micalobia.mixin.PotionAccessor;
import dev.micalobia.mixin.StatusEffectInstanceAccessor;
import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.potion.Potion;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PotionRecipe {
    private static List<BrewingRecipeRegistry.Recipe<Potion>> BASE_POTION_RECIPES;
    private static List<BrewingRecipeRegistry.Recipe<Item>> BASE_ITEM_RECIPES;

    private static boolean cached = false;

    public static void createCache() {
        if (cached) return;
        BetterDatapacks.LOGGER.info("Creating Potion recipe cache");
        BASE_POTION_RECIPES = ImmutableList.copyOf(BrewingRecipeRegistryAccessor.getPotionRecipes());
        BASE_ITEM_RECIPES = ImmutableList.copyOf(BrewingRecipeRegistryAccessor.getItemRecipes());
        cached = true;
    }

    public static void restoreCache() {
        BetterDatapacks.LOGGER.info("Restoring Potion recipe cache");
        BrewingRecipeRegistryAccessor.getPotionRecipes().clear();
        BrewingRecipeRegistryAccessor.getPotionRecipes().addAll(BASE_POTION_RECIPES);
        BrewingRecipeRegistryAccessor.getItemRecipes().clear();
        BrewingRecipeRegistryAccessor.getItemRecipes().addAll(BASE_ITEM_RECIPES);
    }

    private static StatusEffectInstance getHiddenEffect(StatusEffectInstance instance) {
        return ((StatusEffectInstanceAccessor) instance).getHiddenEffect();
    }

    private static String getBaseName(Potion potion) {
        return ((PotionAccessor) potion).getBaseName();
    }

    private static <T> Codec<T> merged(Codec<Either<T, T>> codec) {
        return codec.xmap(x -> x.map(PotionRecipe::self, PotionRecipe::self), Either::left);
    }

    private static Potion newPotion(@Nullable String baseName, List<StatusEffectInstance> effects) {
        return new Potion(baseName, effects.toArray(new StatusEffectInstance[0]));
    }

    private static <T> T self(T t) {
        return t;
    }

    private static DataResult<Item> validatePotionType(Item item) {
        for (var type : BrewingRecipeRegistryAccessor.getPotionTypes()) {
            var stack = new ItemStack(item);
            if (type.test(stack))
                return DataResult.success(item);
        }
        return DataResult.error(() -> "Not a valid potion type!");
    }

    public record ItemType(Item input, Ingredient ingredient, Item output) {
        public static final Codec<ItemType> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Registries.ITEM.getCodec().comapFlatMap(PotionRecipe::validatePotionType, PotionRecipe::self).fieldOf("input").forGetter(ItemType::input),
                BetterDatapacks.INGREDIENT_OR_ITEM_CODEC.fieldOf("ingredient").forGetter(ItemType::ingredient),
                Registries.ITEM.getCodec().comapFlatMap(PotionRecipe::validatePotionType, PotionRecipe::self).fieldOf("output").forGetter(ItemType::output)
        ).apply(instance, ItemType::new));

        private void register() {
            FabricBrewingRecipeRegistry.registerItemRecipe((PotionItem) input, ingredient, (PotionItem) output);
        }

        public static void register(Identifier id, JsonObject json) {
            var recipe = CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(false, x -> {
                throw new JsonParseException(String.format("Failed to parse potion item recipe `%s`; `%s`", id.toString(), x));
            });
            recipe.register();
        }
    }

    public record PotionType(Potion input, Ingredient ingredient, Potion output) {
        private static final Codec<StatusEffectInstance> STATUS_EFFECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Registries.STATUS_EFFECT.getCodec().fieldOf("type").forGetter(StatusEffectInstance::getEffectType),
                Codec.INT.optionalFieldOf("duration", 0).forGetter(StatusEffectInstance::getDuration),
                Codec.INT.optionalFieldOf("amplifier", 0).forGetter(StatusEffectInstance::getAmplifier),
                Codec.BOOL.optionalFieldOf("ambient", false).forGetter(StatusEffectInstance::isAmbient),
                Codec.BOOL.optionalFieldOf("show_particles", true).forGetter(StatusEffectInstance::shouldShowParticles),
                Codec.BOOL.optionalFieldOf("show_icon", true).forGetter(StatusEffectInstance::shouldShowIcon),
                Codec.unit((StatusEffectInstance) null).fieldOf("hidden_effect").forGetter(PotionRecipe::getHiddenEffect),
                StatusEffectInstance.FactorCalculationData.CODEC.optionalFieldOf("factor_calculation_data").forGetter(StatusEffectInstance::getFactorCalculationData)
        ).apply(instance, StatusEffectInstance::new));
        private static final Codec<Potion> POTION_CODEC = merged(Codec.either(
                Registries.POTION.getCodec(),
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.optionalFieldOf("base_name", null).forGetter(PotionRecipe::getBaseName),
                        Codec.list(STATUS_EFFECT_CODEC).fieldOf("effects").forGetter(Potion::getEffects)
                ).apply(instance, PotionRecipe::newPotion))
        ));
        public static final Codec<PotionType> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                POTION_CODEC.fieldOf("input").forGetter(PotionType::input),
                BetterDatapacks.INGREDIENT_OR_ITEM_CODEC.fieldOf("ingredient").forGetter(PotionType::ingredient),
                POTION_CODEC.fieldOf("output").forGetter(PotionType::output)
        ).apply(instance, PotionType::new));

        private void register() {
            FabricBrewingRecipeRegistry.registerPotionRecipe(input, ingredient, output);
        }

        public static void register(Identifier id, JsonObject json) {
            var recipe = CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(false, x -> {
                throw new JsonParseException(String.format("Failed to parse potion recipe `%s`; `%s`", id.toString(), x));
            });
            recipe.register();
        }
    }
}
