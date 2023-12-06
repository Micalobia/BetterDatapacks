package dev.micalobia.mixin;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.advancement.criterion.Criterion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Criteria.class)
public interface CriteriaInvoker {
    @Invoker("register")
    static <T extends Criterion<?>> T register(String id, T criterion) {
        throw new AssertionError();
    }
}
