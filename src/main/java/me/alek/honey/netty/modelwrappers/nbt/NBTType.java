package me.alek.honey.netty.modelwrappers.nbt;

import me.alek.honey.netty.modelwrappers.nbt.lists.NBTList;
import me.alek.honey.netty.modelwrappers.nbt.lists.NBTListOfCompound;
import me.alek.honey.netty.modelwrappers.nbt.lists.NBTListOfList;
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
    @SuppressWarnings("unchecked")
    LIST(9, List.class,
            new ConstructorInvoker<>(castNBTBase(NBTList.class), Object.class, NBTType.class, NBTRoot.class, NBTBase.class)),
    COMPOUND(10, Map.class,
            new ConstructorInvoker<>(castNBTBase(NBTCompound.class), Object.class, NBTRoot.class, NBTBase.class)),
    INTARRAY(11, int[].class),
    LONGARRAY(12, long[].class),
    LISTOFCOMOUND(-1, List.class,
            new ConstructorInvoker<>(castNBTBase(NBTListOfCompound.class), Object.class, NBTRoot.class, NBTBase.class)),
    LISTOFLIST(-1, List.class,
            new ConstructorInvoker<>(castNBTBase(NBTListOfList.class), Object.class, NBTRoot.class, NBTBase.class)),
    UNKNOWN(-1, null);

    private final int typeId;
    private final Class<?> clazz;
    private final ConstructorInvoker<? extends NBTBase<?, ?>> constructorInvoker;

    private static final Map<Class<?>, NBTType> typeLookup;

    static {
        typeLookup = new HashMap<>();

        synchronized (typeLookup) {
            for (NBTType nbtType : values()) {

                if (nbtType == NBTType.UNKNOWN || typeLookup.containsKey(nbtType.getClazz())) {
                    continue;
                }
                typeLookup.put(nbtType.getClazz(), nbtType);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static Class<? extends NBTBase<?, ?>> castNBTBase(Class<?> clazz) {
        try {
            if (NBTBase.class.isAssignableFrom(clazz)) {
                return (Class<? extends NBTBase<?, ?>>) clazz.asSubclass(NBTBase.class);
            }
        } catch (Exception ex) {
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    NBTType(int typeId, Class<?> clazz) {
        this(typeId, clazz, new ConstructorInvoker<>(castNBTBase(NBTElement.class), Object.class, NBTRoot.class, NBTBase.class));
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


    public static NBTType getFromTypeId(byte typeId) {
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
