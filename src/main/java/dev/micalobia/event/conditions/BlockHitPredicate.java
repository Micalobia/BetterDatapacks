package dev.micalobia.event.conditions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public record BlockHitPredicate(
        Optional<Vec3dPredicate> position,
        Optional<EnumSet<Direction>> sides,
        Optional<Boolean> missed,
        Optional<Boolean> insideBlock
) implements Predicate<BlockHitResult> {
    public static final Codec<BlockHitPredicate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Vec3dPredicate.CODEC.optionalFieldOf("position").forGetter(BlockHitPredicate::position),
            Codec.list(Direction.CODEC).xmap(EnumSet::copyOf, List::copyOf).optionalFieldOf("sides").forGetter(BlockHitPredicate::sides),
            Codec.BOOL.optionalFieldOf("missed").forGetter(BlockHitPredicate::missed),
            Codec.BOOL.optionalFieldOf("inside_block").forGetter(BlockHitPredicate::insideBlock)
    ).apply(instance, BlockHitPredicate::new));

    @Override
    public boolean test(BlockHitResult hit) {
        if (position.isPresent() && !position.get().test(hit.getPos())) return false;
        if (sides.isPresent() && !sides.get().contains(hit.getSide())) return false;
        if (missed.isPresent() && (missed.get() ? HitResult.Type.MISS : HitResult.Type.BLOCK) != hit.getType())
            return false;
        return insideBlock.isEmpty() || insideBlock.get() == hit.isInsideBlock();
    }
}
