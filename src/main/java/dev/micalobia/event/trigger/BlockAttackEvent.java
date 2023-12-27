package dev.micalobia.event.trigger;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.micalobia.event.Event;
import dev.micalobia.event.EventCondition;
import dev.micalobia.event.EventContext;
import dev.micalobia.event.conditions.BlockPosPredicate;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LocationPredicate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public final class BlockAttackEvent extends Event<
        BlockAttackEvent.Data,
        BlockAttackEvent.Context,
        BlockAttackEvent.Condition,
        BlockAttackEvent
        > {
    @Override
    public Codec<Condition> conditionCodec() {
        return Condition.CODEC;
    }

    @Override
    public Codec<BlockAttackEvent.Data> dataCodec() {
        return Data.CODEC;
    }

    public ActionResult trigger(PlayerEntity player, World world, Hand hand, BlockPos blockPos, Direction direction) {
        if (world.isClient) return ActionResult.PASS;
        var serverWorld = (ServerWorld) world;
        var server = serverWorld.getServer();
        var serverPlayer = (ServerPlayerEntity) player;
        var context = new Context(server, serverPlayer, blockPos, direction, serverWorld);
        boolean cancel = reduce(context, false, data -> data.cancel, (a, b) -> a | b);
        var result = cancel ? ActionResult.SUCCESS : ActionResult.PASS;
        if (hand == Hand.OFF_HAND) return result;
        trigger(context);
        return result;
    }

    public record Data(At at, boolean cancel) {
        private static final Codec<Data> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                At.CODEC.optionalFieldOf("at", At.PLAYER).forGetter(Data::at),
                Codec.BOOL.optionalFieldOf("cancel", false).forGetter(Data::cancel)
        ).apply(instance, Data::new));
    }

    public static final class Context extends EventContext<Data, Context, Condition> {
        private final ServerPlayerEntity player;
        private final BlockPos position;
        private final Direction side;
        private final ServerWorld world;

        private Context(MinecraftServer server, ServerPlayerEntity player, BlockPos position, Direction side, ServerWorld world) {
            super(server);
            this.player = player;
            this.position = position;
            this.side = side;
            this.world = world;
        }

        @Override
        public ServerCommandSource toSource(Data data) {
            var source = player.getCommandSource();
            if (data.at == At.BLOCK) return source.withPosition(position.toCenterPos());
            return source;
        }
    }

    public record Condition(
            Optional<EntityPredicate> player,
            Optional<LocationPredicate> location,
            Optional<BlockPosPredicate> position,
            Optional<EnumSet<Direction>> sides
    ) implements EventCondition<Context> {
        public static final Codec<Condition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.CODEC.optionalFieldOf("player").forGetter(Condition::player),
                LocationPredicate.CODEC.optionalFieldOf("location").forGetter(Condition::location),
                BlockPosPredicate.CODEC.optionalFieldOf("position").forGetter(Condition::position),
                Codec.list(Direction.CODEC).xmap(EnumSet::copyOf, List::copyOf).optionalFieldOf("sides").forGetter(Condition::sides)
        ).apply(instance, Condition::new));

        @Override
        public boolean check(Context context) {
            if (sides.isPresent() && !sides.get().contains(context.side)) return false;
            if (position.isPresent() && !position.get().test(context.position)) return false;
            var center = context.position.toCenterPos();
            if (player.isPresent() && !player.get().test(context.world, center, context.player)) return false;
            return location.isEmpty() || location.get().test(context.world, center.x, center.y, center.z);
        }
    }

    public enum At implements StringIdentifiable {
        PLAYER("player"),
        BLOCK("block");

        public static final Codec<At> CODEC = StringIdentifiable.createCodec(At::values);

        private final String name;

        At(String name) {
            this.name = name;
        }

        @Override
        public String asString() {
            return name;
        }
    }
}
