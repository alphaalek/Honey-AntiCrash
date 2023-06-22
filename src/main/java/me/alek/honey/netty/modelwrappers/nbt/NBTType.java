package me.alek.honey.netty.modelwrappers.nbt;

import me.alek.honey.netty.utility.reflect.ConstructorInvoker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum NBTType {
    END(0, void.class),
    BYTE(1, byte.class),
    SHORT(2, short.class),
    INT(3, int.class),
    LONG(4, long.class),
    FLOAT(5, float.class),
    DOUBLE(6, double.class),
    BYTEARRAY(7, byte[].class),
    STRING(8, String.class),
    LIST(9, List.class, new ConstructorInvoker<>((Class<NBTBase<?, ?>>) (Class<?>) NBTList.class)),
    COMPOUND(10, Map.class, new ConstructorInvoker<>((Class<NBTBase<?, ?>>) (Class<?>) NBTCompound.class)),
    INTARRAY(11, int[].class),
    UNKNOWN(-1, null);

    private final int typeId;
    private final Class<?> clazz;
    private final ConstructorInvoker<? extends NBTBase<?, ?>> constructorInvoker;

    private static final Map<Class<?>, NBTType> typeLookup = new HashMap<>();

    static {
        initializeTypeLookup();
    }

    NBTType(int typeId, Class<?> clazz) {
        this(typeId, clazz, new ConstructorInvoker<>((Class<? extends NBTBase<?, ?>>) WrappedElement.class));
    }

    NBTType(int typeId, Class<?> clazz, ConstructorInvoker<? extends NBTBase<?, ?>> constructorInvoker) {
        this.typeId = typeId;
        this.clazz = clazz;
        this.constructorInvoker = constructorInvoker;
    }
    public Class<?> getClazz() {
        return clazz;
    }

    public int getTypeId() {
        return typeId;
    }

    public ConstructorInvoker<? extends NBTBase<?, ?>> getConstructorInvoker() {
        return constructorInvoker;
    }

    public static void initializeTypeLookup() {
        if (!typeLookup.isEmpty()) {
            return;
        }
        synchronized (typeLookup) {
            for (NBTType nbtType : values()) {
                typeLookup.put(nbtType.getClazz(), nbtType);
            }
        }
    }

    public static NBTType getFromTypeId(int typeId) {
        try {
            return values()[typeId];
        } catch (Exception ex) {
            return UNKNOWN;
        }
    }

    public static NBTType getFromClass(Class<?> clazz) {
        final NBTType targetType = typeLookup.get(clazz);

        if (targetType != null) {
            return targetType;
        }
        for (Class<?> interfaceClass : clazz.getInterfaces()) {
            NBTType interfaceTargetType = typeLookup.get(interfaceClass);
            if (interfaceTargetType != null) {
                return interfaceTargetType;
            }
        }
        return UNKNOWN;
    }

}
