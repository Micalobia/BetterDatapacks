package dev.micalobia;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import dev.micalobia.command.calculate.CalculateCommand;
import dev.micalobia.mixin.CriteriaInvoker;
import dev.micalobia.recipe.PotionRecipe;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BetterDatapacks implements ModInitializer {
    public static final String MODID = "better_datapacks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public static final Codec<Ingredient> INGREDIENT_OR_ITEM_CODEC = Codec.either(Registries.ITEM.getCodec(), Ingredient.DISALLOW_EMPTY_CODEC).xmap(
            either -> either.map(Ingredient::ofItems, x -> x),
            Either::right
    );

    public static final BlockBrokenCriterion BLOCK_BROKEN = CriteriaInvoker.register(idString("block_broken"), new BlockBrokenCriterion());

    @Override
    public void onInitialize() {
        PlayerBlockBreakEvents.AFTER.register(BLOCK_BROKEN::trigger);
        ServerLifecycleEvents.START_DATA_PACK_RELOAD.register((server, manager) -> PotionRecipe.restoreCache());
        CommandRegistrationCallback.EVENT.register(CalculateCommand::register);
    }

    public static Identifier id(String path) {
        return new Identifier(MODID, path);
    }

    public static String idString(String path) {
        return id(path).toString();
    }
}