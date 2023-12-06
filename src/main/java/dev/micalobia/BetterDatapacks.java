package dev.micalobia;

import dev.micalobia.mixin.CriteriaInvoker;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BetterDatapacks implements ModInitializer {
    public static final String MODID = "better_datapacks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public static final BlockBrokenCriterion BLOCK_BROKEN = CriteriaInvoker.register(idString("block_broken"), new BlockBrokenCriterion());

    @Override
    public void onInitialize() {
        PlayerBlockBreakEvents.AFTER.register(BLOCK_BROKEN::trigger);
    }

    public static Identifier id(String path) {
        return new Identifier(MODID, path);
    }

    public static String idString(String path) {
        return id(path).toString();
    }
}