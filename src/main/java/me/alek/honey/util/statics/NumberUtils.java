package me.alek.honey.util.statics;

public class NumberUtils {

    public static boolean isNaN(double value) {
        return Double.isNaN(value);
    }

    public static boolean isNaN(float value) {
        return Float.isNaN(value);
    }

    public static boolean isInvalid(double value) {
        return value == Double.MAX_VALUE || value == Double.MIN_VALUE || isNaN(value);
    }

    public static boolean isInvalid(double... values) {
        for (double value : values) {
            if (isInvalid(value)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInvalid(float value) {
        return value == Float.MAX_VALUE || value == Integer.MIN_VALUE || isNaN(value);
    }

    public static boolean isInvalid(float... values) {
        for (float value : values) {
            if (isInvalid(value)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInvalid(int[] values) {
        for (int value : values) {
            if (isInvalid((double) value)) {
                return true;
            }
        }
        return false;
    }

    public static double format(double value) {
        return format(value, 2);
    }

    public static Number format(Number value) {
        if (value instanceof Integer) return value;

        return format(value.doubleValue(), 2);
    }

    public static double format(double value, int decimals) {
        double m = Math.pow(10, decimals);
        return Math.ceil(value * m) / m;
    }

}
