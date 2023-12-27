package dev.micalobia.event.trigger;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.micalobia.BetterDatapacks;
import dev.micalobia.CodecUtility;
import dev.micalobia.event.Event;
import dev.micalobia.event.EventCondition;
import dev.micalobia.event.EventContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.item.ItemPredicate;
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
        var item = player.getStackInHand(hand);
        var context = new Context(server, player, entity, item, hand);
        boolean cancel = reduce(context, false, data -> data.cancel, (a, b) -> a | b);
        var result = cancel ? ActionResult.SUCCESS : ActionResult.PASS;
        if (entityHitResult != null) return result;
        trigger(context);
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
        private final ItemStack item;
        private final Hand hand;

        protected Context(MinecraftServer server, Entity interactor, Entity interactee, ItemStack item, Hand hand) {
            super(server);
            this.interactor = interactor;
            this.interactee = interactee;
            this.item = item;
            this.hand = hand;
        }

        @Override
        public ServerCommandSource toSource(Data data) {
            var executor = data.executor == Executor.INTERACTOR ? interactor : interactee;
            return executor.getCommandSource();
        }
    }

    public record Conditions(Optional<EntityPredicate> interactor,
                             Optional<EntityPredicate> interactee,
                             Optional<ItemPredicate> item,
                             Optional<Hand> hand
    ) implements EventCondition<Context> {
        private static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.CODEC.optionalFieldOf("interactor").forGetter(Conditions::interactor),
                EntityPredicate.CODEC.optionalFieldOf("interactee").forGetter(Conditions::interactee),
                ItemPredicate.CODEC.optionalFieldOf("item").forGetter(Conditions::item),
                CodecUtility.HAND_CODEC.optionalFieldOf("hand").forGetter(Conditions::hand)
        ).apply(instance, Conditions::new));

        @Override
        public boolean check(Context context) {
            if (hand.isPresent() && (hand.get() != context.hand)) return false;
            if (this.item.isPresent() && !this.item.get().test(context.item)) return false;
            var serverWorld = (ServerWorld) context.interactor.getWorld();
            if (interactor.isPresent() && !interactor.get().test(serverWorld, context.interactor.getPos(), context.interactor))
                return false;
            return interactee.isEmpty() || interactee.get().test(serverWorld, context.interactee.getPos(), context.interactee);
        }
    }

    public record Data(Executor executor, boolean cancel) {
        private static final Codec<Data> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Executor.CODEC.optionalFieldOf("executor", Executor.INTERACTOR).forGetter(Data::executor),
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
