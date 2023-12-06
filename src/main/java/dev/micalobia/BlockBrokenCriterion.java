package dev.micalobia;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.advancement.criterion.AbstractCriterionConditions;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.BlockPredicate;
import net.minecraft.predicate.entity.AdvancementEntityPredicateDeserializer;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class BlockBrokenCriterion extends AbstractCriterion<BlockBrokenCriterion.Conditions> {


    @Override
    protected Conditions conditionsFromJson(JsonObject obj, Optional<LootContextPredicate> predicate, AdvancementEntityPredicateDeserializer predicateDeserializer) {
        var block = obj.get("block");
        BlockPredicate blockPredicate = null;
        if (block != null) {
            var result = BlockPredicate.CODEC.parse(JsonOps.INSTANCE, block);
            blockPredicate = result.resultOrPartial(BetterDatapacks.LOGGER::error).orElseThrow(() -> new JsonParseException("Failed to parse `block` field"));
        }
        return new Conditions(predicate, Optional.ofNullable(blockPredicate));
    }

    public void trigger(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) throw new AssertionError();
        this.trigger(serverPlayer, conditions -> conditions.test(state, blockEntity));
    }

    public static class Conditions extends AbstractCriterionConditions {
        private final Optional<BlockPredicate> block;

        public Conditions(Optional<LootContextPredicate> playerPredicate, Optional<BlockPredicate> block) {
            super(playerPredicate);
            this.block = block;
        }

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
