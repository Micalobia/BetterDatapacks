package dev.micalobia.event.trigger;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.micalobia.event.Event;
import dev.micalobia.event.EventCondition;
import dev.micalobia.event.EventContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

import java.util.Optional;

public class EntityInteractionEvent extends Event<
        EntityInteractionEvent.Data,
        EntityInteractionEvent.Context,
        EntityInteractionEvent.Conditions,
        EntityInteractionEvent
        > {

    public ActionResult trigger(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult entityHitResult) {
        if (world.isClient) return ActionResult.PASS;
        var server = ((ServerWorld) world).getServer();
        var context = new Context(server, player, entity);
        boolean cancel = reduce(context, false, data -> data.cancel, (a, b) -> a | b);
        var result = cancel ? ActionResult.SUCCESS : ActionResult.PASS;
        if (hand == Hand.OFF_HAND) return result;
        if (entityHitResult == null) return result;
        if (player.isSpectator()) return result;
        trigger(new Context(server, player, entity));
        return result;
    }

    @Override
    public Codec<Conditions> conditionCodec() {
        return Conditions.CODEC;
    }

    @Override
    public Codec<Data> dataCodec() {
        return Data.CODEC;
    }

    public static class Context extends EventContext<Data, Context, Conditions> {

        private final Entity interactee;
        private final Entity interactor;

        protected Context(MinecraftServer server, Entity interactor, Entity interactee) {
            super(server);
            this.interactor = interactor;
            this.interactee = interactee;
        }

        @Override
        public ServerCommandSource toSource(Data data) {
            var executor = data.executor == Executor.INTERACTOR ? interactor : interactee;
            return executor.getCommandSource();
        }
    }

    public record Conditions(Optional<EntityPredicate> interactorPredicate,
                             Optional<EntityPredicate> interacteePredicate
    ) implements EventCondition<Context> {
        private static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.CODEC.optionalFieldOf("interactor").forGetter(Conditions::interactorPredicate),
                EntityPredicate.CODEC.optionalFieldOf("interactee").forGetter(Conditions::interacteePredicate)
        ).apply(instance, Conditions::new));

        @Override
        public boolean check(Context context) {
            var serverWorld = (ServerWorld) context.interactor.getWorld();
            if (interactorPredicate.isPresent() && !interactorPredicate.get().test(serverWorld, context.interactor.getPos(), context.interactor)) {
                return false;
            }
            return interacteePredicate.isEmpty() || interacteePredicate.get().test(serverWorld, context.interactee.getPos(), context.interactee);
        }
    }

    public record Data(Executor executor, boolean cancel) {
        private static final Codec<Data> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Executor.CODEC.fieldOf("executor").forGetter(Data::executor),
                Codec.BOOL.optionalFieldOf("cancel", false).forGetter(Data::cancel)
        ).apply(instance, Data::new));
    }

    public enum Executor implements StringIdentifiable {
        INTERACTOR("interactor"),
        INTERACTEE("interactee");

        public static final Codec<Executor> CODEC = StringIdentifiable.createCodec(Executor::values);

        private final String name;

        Executor(String name) {
            this.name = name;
        }

        @Override
        public String asString() {
            return name;
        }
    }
}
