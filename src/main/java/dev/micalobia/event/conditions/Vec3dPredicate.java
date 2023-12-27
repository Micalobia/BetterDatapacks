package dev.micalobia.event.conditions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.predicate.NumberRange;
import net.minecraft.util.math.Vec3d;

import java.util.function.Predicate;

public record Vec3dPredicate(
        NumberRange.DoubleRange x,
        NumberRange.DoubleRange y,
        NumberRange.DoubleRange z
) implements Predicate<Vec3d> {
    public static final Codec<Vec3dPredicate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            NumberRange.DoubleRange.CODEC.optionalFieldOf("x", NumberRange.DoubleRange.ANY).forGetter(Vec3dPredicate::x),
            NumberRange.DoubleRange.CODEC.optionalFieldOf("y", NumberRange.DoubleRange.ANY).forGetter(Vec3dPredicate::y),
            NumberRange.DoubleRange.CODEC.optionalFieldOf("z", NumberRange.DoubleRange.ANY).forGetter(Vec3dPredicate::z)
    ).apply(instance, Vec3dPredicate::new));

    @Override
    public boolean test(Vec3d pos) {
        return x.test(pos.x) && y.test(pos.y) && z.test(pos.z);
    }
}
