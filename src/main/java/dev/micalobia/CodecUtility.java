package dev.micalobia;

import com.google.common.collect.EnumHashBiMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.util.Hand;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;

public class CodecUtility {
    public static <E extends Enum<E>> Codec<E> enumCodec(Class<E> klass, Supplier<E[]> valueSupplier, Function<E, String> nameSupplier) {
        var enums = valueSupplier.get();
        var map = EnumHashBiMap.create(klass);
        Arrays.stream(enums).forEach(x -> map.put(x, nameSupplier.apply(x)));
        return Codec.STRING.comapFlatMap(s -> {
            var ret = map.inverse().get(s);
            if (ret == null) return DataResult.error(() -> String.format("Invalid value `%s`", s));
            return DataResult.success(ret);
        }, nameSupplier);
    }

    public static Codec<Hand> HAND_CODEC = CodecUtility.enumCodec(Hand.class, Hand::values, x -> x == Hand.MAIN_HAND ? "mainhand" : "offhand");
}
