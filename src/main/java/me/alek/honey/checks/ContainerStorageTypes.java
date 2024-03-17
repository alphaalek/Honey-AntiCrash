package me.alek.honey.checks;

import java.util.HashMap;
import java.util.Map;

public class ContainerStorageTypes {

    private static final Map<Class<?>, Integer> INDICES = new HashMap<>();
    private static int index = 8;

    public static int getIndex(Class<?> clazz) {
        return INDICES.computeIfAbsent(clazz, (d) -> ++index);
    }

    public static int getIndexForBoolean() {
        return 0;
    }

    public static int getIndexForString() {
        return 1;
    }

    public static int getIndexForChar() {
        return 2;
    }

    public static int getIndexForInt() {
        return 3;
    }

    public static int getIndexForFloat() {
        return 4;
    }

    public static int getIndexForByte() {
        return 5;
    }

    public static int getIndexForDouble() {
        return 6;
    }

    public static int getIndexForLong() {
        return 7;
    }

    public static int getIndexForLocation() {
        return 8;
    }
}
