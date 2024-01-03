package dev.micalobia.event.conditions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.predicate.NumberRange;
import net.minecraft.util.math.BlockPos;

import java.util.function.Predicate;

public record BlockPosPredicate(
        NumberRange.IntRange x,
        NumberRange.IntRange y,
        NumberRange.IntRange z
) implements Predicate<BlockPos> {
    public static final Codec<BlockPosPredicate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            NumberRange.IntRange.CODEC.optionalFieldOf("x", NumberRange.IntRange.ANY).forGetter(BlockPosPredicate::x),
            NumberRange.IntRange.CODEC.optionalFieldOf("y", NumberRange.IntRange.ANY).forGetter(BlockPosPredicate::y),
            NumberRange.IntRange.CODEC.optionalFieldOf("z", NumberRange.IntRange.ANY).forGetter(BlockPosPredicate::z)
    ).apply(instance, BlockPosPredicate::new));

    @Override
    public boolean test(BlockPos pos) {
        return x.test(pos.getX()) && y.test(pos.getY()) && z.test(pos.getZ());
    }
}
