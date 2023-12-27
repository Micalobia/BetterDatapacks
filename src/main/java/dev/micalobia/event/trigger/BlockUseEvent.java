package dev.micalobia.event.trigger;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.micalobia.event.Event;
import dev.micalobia.event.EventCondition;
import dev.micalobia.event.EventContext;
import dev.micalobia.event.conditions.BlockHitPredicate;
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
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Optional;

public final class BlockUseEvent extends Event<
        BlockUseEvent.Data,
        BlockUseEvent.Context,
        BlockUseEvent.Condition,
        BlockUseEvent
        > {
    @Override
    public Codec<Condition> conditionCodec() {
        return Condition.CODEC;
    }

    @Override
    public Codec<BlockUseEvent.Data> dataCodec() {
        return Data.CODEC;
    }

    public ActionResult trigger(PlayerEntity player, World world, Hand hand, BlockHitResult blockHitResult) {
        if (world.isClient) return ActionResult.PASS;
        var serverWorld = (ServerWorld) world;
        var server = serverWorld.getServer();
        var serverPlayer = (ServerPlayerEntity) player;
        var context = new Context(server, serverPlayer, blockHitResult, serverWorld);
        boolean cancel = reduce(context, false, data -> data.cancel, (a, b) -> a | b);
        var result = cancel ? ActionResult.SUCCESS : ActionResult.PASS;
        if (hand == Hand.OFF_HAND) return result;
        trigger(context);
        if (result != ActionResult.PASS) serverPlayer.currentScreenHandler.syncState();
        return result;
    }

    public record Data(At at, boolean cancel) {
        private static final Codec<Data> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                At.CODEC.fieldOf("at").forGetter(Data::at),
                Codec.BOOL.optionalFieldOf("cancel", false).forGetter(Data::cancel)
        ).apply(instance, Data::new));
    }

    public static final class Context extends EventContext<Data, Context, Condition> {
        private final ServerPlayerEntity player;
        private final BlockHitResult hitResult;
        private final ServerWorld world;

        private Context(MinecraftServer server, ServerPlayerEntity player, BlockHitResult hitResult, ServerWorld world) {
            super(server);
            this.player = player;
            this.hitResult = hitResult;
            this.world = world;
        }

        @Override
        public ServerCommandSource toSource(Data data) {
            var source = player.getCommandSource();
            return switch (data.at) {
                case HIT -> source.withPosition(hitResult.getPos());
                case BLOCK -> source.withPosition(hitResult.getBlockPos().toCenterPos());
                default -> source;
            };
        }
    }

    public record Condition(
            Optional<EntityPredicate> player,
            Optional<BlockHitPredicate> hit,
            Optional<LocationPredicate> location
    ) implements EventCondition<Context> {
        public static final Codec<Condition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.CODEC.optionalFieldOf("player").forGetter(Condition::player),
                BlockHitPredicate.CODEC.optionalFieldOf("hit").forGetter(Condition::hit),
                LocationPredicate.CODEC.optionalFieldOf("location").forGetter(Condition::location)
        ).apply(instance, Condition::new));

        @Override
        public boolean check(Context context) {
            if (player.isPresent() && !player.get().test(context.world, context.hitResult.getPos(), context.player))
                return false;
            if (hit.isPresent() && !hit.get().test(context.hitResult)) return false;
            var type = context.hitResult.getType();
            Vec3d pos;
            if (type == HitResult.Type.BLOCK) pos = context.hitResult.getBlockPos().toCenterPos();
            else pos = Vec3d.ZERO;
            return location.isEmpty() || location.get().test(context.world, pos.x, pos.y, pos.z);
        }
    }

    public enum At implements StringIdentifiable {
        PLAYER("player"),
        BLOCK("block"),
        HIT("hit");

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
