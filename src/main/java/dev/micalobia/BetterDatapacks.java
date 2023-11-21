package dev.micalobia;

import net.fabricmc.api.ModInitializer;

import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BetterDatapacks implements ModInitializer {
    public static final String MODID = "better_datapacks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    @Override
    public void onInitialize() {
    }

    public static Identifier id(String path) {
        return new Identifier(MODID, path);
    }

    public static String idString(String path) {
        return id(path).toString();
    }
}