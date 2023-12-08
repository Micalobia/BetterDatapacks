package dev.micalobia;

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
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PotionRecipe {
    private static final Codec<ItemType> ITEM_TYPE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Registries.ITEM.getCodec().comapFlatMap(PotionRecipe::validatePotionType, PotionRecipe::self).fieldOf("input").forGetter(ItemType::input),
            Registries.ITEM.getCodec().fieldOf("ingredient").forGetter(ItemType::ingredient),
            Registries.ITEM.getCodec().comapFlatMap(PotionRecipe::validatePotionType, PotionRecipe::self).fieldOf("output").forGetter(ItemType::output)
    ).apply(instance, ItemType::new));

    public static void register(Identifier id, JsonObject json) {
        var either = CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(false, x -> {
            throw new JsonParseException(String.format("Failed to parse potion `%s`; `%s`", id.toString(), x));
        });
        either.ifLeft(ItemType::register).ifRight(PotionType::register);
    }


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
    private static final Codec<PotionType> POTION_TYPE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            POTION_CODEC.fieldOf("input").forGetter(PotionType::input),
            Registries.ITEM.getCodec().fieldOf("ingredient").forGetter(PotionType::ingredient),
            POTION_CODEC.fieldOf("output").forGetter(PotionType::output)
    ).apply(instance, PotionType::new));

    public static final Codec<Either<ItemType, PotionType>> CODEC = Codec.either(ITEM_TYPE_CODEC, POTION_TYPE_CODEC);

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

    public record ItemType(Item input, Item ingredient, Item output) {
        public void register() {
            BrewingRecipeRegistryAccessor.registerItemRecipe(input, ingredient, output);
        }
    }

    public record PotionType(Potion input, Item ingredient, Potion output) {
        public void register() {
            BrewingRecipeRegistryAccessor.registerPotionRecipe(input, ingredient, output);
        }
    }
}
