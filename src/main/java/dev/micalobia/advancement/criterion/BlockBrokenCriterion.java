package dev.micalobia.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.BlockPredicate;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class BlockBrokenCriterion extends AbstractCriterion<BlockBrokenCriterion.Conditions> {
    @Override
    public Codec<Conditions> getConditionsCodec() {
        return Conditions.CODEC;
    }

    public void trigger(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) throw new AssertionError();
        this.trigger(serverPlayer, conditions -> conditions.test(state, blockEntity));
    }

    public record Conditions(Optional<LootContextPredicate> player,
                             Optional<BlockPredicate> block) implements AbstractCriterion.Conditions {
        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC.optionalFieldOf("player").forGetter(Conditions::player),
                BlockPredicate.CODEC.optionalFieldOf("block").forGetter(Conditions::block)
        ).apply(instance, Conditions::new));

        public boolean test(BlockState state, @Nullable BlockEntity blockEntity) {
            if (block.isEmpty()) return true;
            var predicate = block.get();
            if (predicate.tag().isPresent() && !state.isIn(predicate.tag().get())) return false;
            if (predicate.blocks().isPresent() && !state.isIn(predicate.blocks().get())) return false;
            if (predicate.state().isPresent() && !(predicate.state().get()).test(state)) return false;
            if (predicate.nbt().isPresent())
                return blockEntity != null && (predicate.nbt().get()).test(blockEntity.createNbtWithIdentifyingData());
            return true;
        }
    }
}
