package dev.micalobia.command.calculate;

import java.util.function.BinaryOperator;

public enum MathUtility {
    ;

    public static Number sin(Number value) {
        return Math.sin(value.doubleValue());
    }

    public static Number cos(Number value) {
        return Math.cos(value.doubleValue());
    }

    public static Number tan(Number value) {
        return Math.tan(value.doubleValue());
    }

    public static Number asin(Number value) {
        return Math.asin(value.doubleValue());
    }

    public static Number acos(Number value) {
        return Math.acos(value.doubleValue());
    }

    public static Number atan(Number value) {
        return Math.atan(value.doubleValue());
    }

    public static Number sinh(Number value) {
        return Math.sinh(value.doubleValue());
    }

    public static Number cosh(Number value) {
        return Math.cosh(value.doubleValue());
    }

    public static Number tanh(Number value) {
        return Math.tanh(value.doubleValue());
    }

    public static Number sqrt(Number value) {
        return Math.sqrt(value.doubleValue());
    }

    public static Number floor(Number value) {
        return Math.floor(value.doubleValue());
    }

    public static Number ceil(Number value) {
        return Math.ceil(value.doubleValue());
    }

    public static Number abs(Number value) {
        if (value instanceof Integer val) return Math.abs(val);
        if (value instanceof Long val) return Math.abs(val);
        if (value instanceof Float val) return Math.abs(val);
        return Math.abs(value.doubleValue());
    }

    private static Number wideOperation(Number left, Number right, BinaryOperator<Integer> intOp, BinaryOperator<Long> longOp, BinaryOperator<Float> floatOp, BinaryOperator<Double> doubleOp) {
        if (left instanceof Double || right instanceof Double)
            return doubleOp.apply(left.doubleValue(), right.doubleValue());
        if (left instanceof Float || right instanceof Float)
            return floatOp.apply(left.floatValue(), right.floatValue());
        if (left instanceof Long || right instanceof Long)
            return longOp.apply(left.longValue(), right.longValue());
        if (left instanceof Integer || right instanceof Integer)
            return intOp.apply(left.intValue(), right.intValue());
        return doubleOp.apply(left.doubleValue(), right.doubleValue());
    }

    public static Number add(Number left, Number right) {
        return wideOperation(left, right, Integer::sum, Long::sum, Float::sum, Double::sum);
    }

    public static Number subtract(Number left, Number right) {
        return wideOperation(left, right, (a, b) -> a - b, (a, b) -> a - b, (a, b) -> a - b, (a, b) -> a - b);
    }

    public static Number multiply(Number left, Number right) {
        return wideOperation(left, right, (a, b) -> a * b, (a, b) -> a * b, (a, b) -> a * b, (a, b) -> a * b);
    }

    public static Number divide(Number left, Number right) {
        return wideOperation(left, right, (a, b) -> a / b, (a, b) -> a / b, (a, b) -> a / b, (a, b) -> a / b);
    }

    public static Number modulus(Number left, Number right) {
        return wideOperation(left, right, (a, b) -> a % b, (a, b) -> a % b, (a, b) -> a % b, (a, b) -> a % b);
    }

    public static Number atan2(Number left, Number right) {
        return Math.atan2(left.doubleValue(), right.doubleValue());
    }

    public static Number power(Number left, Number right) {
        return Math.pow(left.doubleValue(), right.doubleValue());
    }

    public static Number min(Number left, Number right) {
        return wideOperation(left, right, Math::min, Math::min, Math::min, Math::min);
    }

    public static Number max(Number left, Number right) {
        return wideOperation(left, right, Math::max, Math::max, Math::max, Math::max);
    }
}
