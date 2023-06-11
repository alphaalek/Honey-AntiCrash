package me.alek.honey.utility;

public class NumberConversions {

    public static boolean isInfinite(double value) {
        return Double.isInfinite(value) || value > Double.MAX_VALUE;
    }

    public static boolean isInfinite(float value) {
        return Float.isInfinite(value) || value > Float.MAX_VALUE;
    }

    public static boolean isNaN(double value) {
        return Double.isNaN(value);
    }

    public static boolean isNaN(float value) {
        return Float.isNaN(value);
    }

    public static boolean isInvalid(double value) {
        return isInfinite(value) || isNaN(value);
    }

    public static boolean isInvalid(float value) {
        return isInfinite(value) || isNaN(value);

    }
}
