package dev.micalobia.event;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import dev.micalobia.BetterDatapacks;
import dev.micalobia.event.trigger.BlockAttackEvent;
import dev.micalobia.event.trigger.EntityInteractionEvent;
import dev.micalobia.event.trigger.BlockUseEvent;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.profiler.Profiler;

import java.util.HashMap;
import java.util.Map;

public final class Events {
    @SuppressWarnings("rawtypes")
    private static final Map<Identifier, Event> EVENTS = new HashMap<>();

    public static final EntityInteractionEvent ENTITY_USE = register(BetterDatapacks.id("entity_use"), new EntityInteractionEvent());
    public static final EntityInteractionEvent ENTITY_ATTACK = register(BetterDatapacks.id("entity_attack"), new EntityInteractionEvent());
    public static final BlockUseEvent USE_BLOCK = register(BetterDatapacks.id("use_block"), new BlockUseEvent());
    public static final BlockAttackEvent ATTACK_BLOCK = register(BetterDatapacks.id("attack_block"), new BlockAttackEvent());

    private Events() {
    }

    public static <D, X extends EventContext<D, X, C>, C extends EventCondition<X>, E extends Event<D, X, C, E>> E register(Identifier id, E event) {
        EVENTS.put(id, event);
        return event;
    }

    public static void init() {
        UseEntityCallback.EVENT.register(ENTITY_USE::trigger);
        AttackEntityCallback.EVENT.register(ENTITY_ATTACK::trigger);
        UseBlockCallback.EVENT.register(USE_BLOCK::trigger);
        AttackBlockCallback.EVENT.register(ATTACK_BLOCK::trigger);
    }

    public static class ReloadListener extends JsonDataLoader implements IdentifiableResourceReloadListener {
        private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

        public ReloadListener() {
            super(GSON, "events");
        }

        @Override
        public Identifier getFabricId() {
            return BetterDatapacks.id("events");
        }

        @Override
        protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, Profiler profiler) {
            EVENTS.values().forEach(Event::clearTriggers);
            for (var pair : prepared.entrySet()) {
                var id = pair.getKey();
                var json = pair.getValue();
                if (!json.isJsonObject()) {
                    BetterDatapacks.LOGGER.error("Parsing error loading supplier {}; Not a JSON object!", id);
                    continue;
                }
                var obj = json.getAsJsonObject();
                var type = JsonHelper.getString(obj, "type");
                Identifier typeId;
                try {
                    typeId = new Identifier(type);
                } catch (InvalidIdentifierException ignored) {
                    BetterDatapacks.LOGGER.error("Parsing error loading supplier {}; Unknown type `{}`", id, type);
                    continue;
                }
                if (!EVENTS.containsKey(typeId)) {
                    BetterDatapacks.LOGGER.error("Parsing error loading supplier {}; Unknown type `{}`", id, type);
                    continue;
                }
                var event = EVENTS.get(typeId);
                var trigger = event.parseTrigger(id, obj);
                if (trigger.isEmpty()) continue;
                //noinspection unchecked,rawtypes
                event.addTrigger((Event.TriggerableEvent) trigger.get());
                BetterDatapacks.LOGGER.info(id.toString());
            }
        }
    }
}
