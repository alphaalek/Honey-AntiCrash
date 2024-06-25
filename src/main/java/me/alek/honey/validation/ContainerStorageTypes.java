package me.alek.honey.validation;

import java.util.HashMap;
import java.util.Map;

public class ContainerStorageTypes {

    private static final Map<Class<?>, Integer> INDICES = new HashMap<>();
    private static int index = 8;

    public static int getIndex(Class<?> clazz) {
        return INDICES.computeIfAbsent(clazz, (d) -> ++index);
    }

    public static int BOOLEAN = 0;
    public static int STRING = 1;
    public static int CHAR = 2;
    public static int INT = 3;
    public static int FLOAT = 4;
    public static int BYTE = 5;
    public static int DOUBLE = 6;
    public static int LONG = 7;
    public static int LOCATION = 8;
}
