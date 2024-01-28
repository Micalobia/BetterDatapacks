package dev.micalobia.event;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.micalobia.BetterDatapacks;
import net.minecraft.server.function.LazyContainer;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class Event<D, X extends EventContext<D, X, C>, C extends EventCondition<X>, E extends Event<D, X, C, E>> {
    private final Codec<Parsed<D, X, C>> CODEC = RecordCodecBuilder.create(instance -> instance.group(Identifier.CODEC.fieldOf("function").forGetter(Parsed::function), dataCodec().fieldOf("data").forGetter(Parsed::data), conditionCodec().fieldOf("conditions").forGetter(Parsed::condition)).apply(instance, Parsed::new));

    private final List<TriggerableEvent<D, X, C, E>> triggerableEvents = new ArrayList<>();

    public final void clearTriggers() {
        triggerableEvents.clear();
    }

    public final void addTrigger(TriggerableEvent<D, X, C, E> trigger) {
        triggerableEvents.add(trigger);
    }

    private static <T> void doNothing(T data) {
    }

    public final void trigger(X context) {
        trigger(context, Event::doNothing);
    }

    public final void trigger(X context, @NotNull Consumer<D> processSuccess) {
        triggerableEvents.forEach(event -> event.trigger(context, processSuccess));
    }

    public <T> T reduce(X context, T start, Function<D, T> map, BinaryOperator<T> reducer) {
        var reduced = triggerableEvents.stream().filter(x -> x.canReduce(context)).map(x -> x.data).map(map).reduce(reducer);
        return reduced.map(x -> reducer.apply(x, start)).orElse(start);
    }


    public abstract Codec<C> conditionCodec();

    public abstract Codec<D> dataCodec();

    public final Optional<TriggerableEvent<D, X, C, E>> parseTrigger(Identifier id, JsonObject json) {
        var pair = CODEC.parse(JsonOps.INSTANCE, json).get();
        if (pair.right().isPresent()) {
            BetterDatapacks.LOGGER.error("Parsing error loading event {}; {}", id, pair.right().get().message());
            return Optional.empty();
        }
        var event = pair.left().orElseThrow();
        return Optional.of(new TriggerableEvent<>(this, event.data, event.condition, new LazyContainer(event.function)));
    }

    public record TriggerableEvent<D, X extends EventContext<D, X, C>, C extends EventCondition<X>, E extends Event<D, X, C, E>>(
            Event<D, X, C, E> event, D data, C condition, LazyContainer function) {
        public void trigger(X context, Consumer<D> processSuccess) {
            var manager = context.getServer().getCommandFunctionManager();
            var func = function.get(manager);
            if (func.isEmpty()) {
                BetterDatapacks.LOGGER.error(String.format("Couldn't find function `%s` for event", function.getId()));
                return;
            }
            if (!condition.check(context)) return;
            // TODO: Do the macro substitution, they changed a lot from 1.20.2 to 1.20.4 so not sure how to do it anymore
            var source = context.toSource(data).withSilent().withLevel(2);
            var commandFunction = func.get();
            manager.execute(commandFunction, source);
        }

        private boolean canReduce(X context) {
            var manager = context.getServer().getCommandFunctionManager();
            var func = function.get(manager);
            if (func.isEmpty()) return false;
            return condition.check(context);
        }
    }

    private record Parsed<D, X, C extends EventCondition<X>>(Identifier function, D data, C condition) {
    }
}
