package me.alek.honey.netty.nbt;

import me.alek.honey.netty.nbt.array.GeneticArrayTypes;
import me.alek.honey.netty.nbt.array.NBTArray;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.ConstructorInvoker;
import me.alek.honey.util.reflection.Reflection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public enum NBTType {
    END(0, getNBTClass("NBTTagEnd"), void.class),
    BYTE(1, getNBTClass("NBTTagByte"), byte.class),
    SHORT(2, getNBTClass("NBTTagShort"), short.class),
    INT(3, getNBTClass("NBTTagInt"), int.class),
    LONG(4, getNBTClass("NBTTagLong"), long.class),
    FLOAT(5, getNBTClass("NBTTagFloat"), float.class),
    DOUBLE(6, getNBTClass("NBTTagDouble"), double.class),
    ARRAY_OF_BYTE(7, getNBTClass("NBTTagByteArray"), byte[].class),
    STRING(8, getNBTClass("NBTTagString"), String.class),
    ARRAY(9, getNBTClass("NBTTagList"),
            new ConstructorInvoker<>(getNBTBaseClass(NBTArray.class), Object.class, NBTType.class, NBTRoot.class, NBTBase.class, boolean.class), List.class, byte.class),
    COMPOUND(10, getNBTClass("NBTTagCompound"),
            new ConstructorInvoker<>(getNBTBaseClass(NBTCompound.class), Object.class, NBTRoot.class, NBTBase.class, boolean.class), Map.class, boolean.class),
    ARRAY_OF_INT(11, getNBTClass("NBTTagIntArray"), int[].class),
    ARRAY_OF_LONG(12, NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_12_2) ? getNBTClass("NBTTagLongArray") : null, long[].class),
    ARRAY_OF_COMPOUND(9, getNBTClass("NBTTagList"),
            new ConstructorInvoker<>(getNBTBaseClass(GeneticArrayTypes.NBTArrayOfArray.class), Object.class, NBTRoot.class, NBTBase.class, boolean.class), List.class, byte.class),
    ARRAY_OF_ARRAY(9, getNBTClass("NBTTagList"),
            new ConstructorInvoker<>(getNBTBaseClass(GeneticArrayTypes.NBTArrayOfArray.class), Object.class, NBTRoot.class, NBTBase.class, boolean.class), List.class, byte.class),
    UNKNOWN(-1, null, null);

    private final int typeId;
    private final Class<?> clazz;
    private final ConstructorInvoker<? extends NBTBase<?, ?>> wrappedInvoker;
    private final ConstructorInvoker<?> baseInvoker;
    private final ConstructorInvoker<?> elementBaseInvoker;
    private final Supplier<Object> getterWithoutParams;

    private static final Map<Class<?>, NBTType> typeLookup = new HashMap<>();

    private static Class<?> getNBTClass(String name) {
        return Reflection.getUnsureClass("{nms}." + name, "{nms}.nbt." + name);
    }

    static {
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
    public static Class<? extends NBTBase<?, ?>> getNBTBaseClass(Class<?> clazz) {
        try {
            if (NBTBase.class.isAssignableFrom(clazz)) {
                return (Class<? extends NBTBase<?, ?>>) clazz.asSubclass(NBTBase.class);
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    NBTType(int typeId, Class<?> baseClass, Class<?> clazz) {
        this(typeId, baseClass, new ConstructorInvoker<>(getNBTBaseClass(NBTElement.class), Object.class, NBTRoot.class, NBTBase.class, boolean.class), clazz);
    }

    NBTType(int typeId, Class<?> baseClass, ConstructorInvoker<? extends NBTBase<?, ?>> constructorInvoker, Class<?> clazz, Class<?>... params) {
        this.typeId = typeId;
        this.clazz = clazz;

        if (baseClass == null) {
            this.wrappedInvoker = null;
            this.baseInvoker = null;
            this.elementBaseInvoker = null;
            this.getterWithoutParams = null;
        } else {
            this.wrappedInvoker = constructorInvoker;

            ConstructorInvoker<?> baseInvoker = null;
            try {
                baseInvoker = Reflection.getConstructor(baseClass);
            } catch (Throwable ignored) {
            }
            this.baseInvoker = baseInvoker;

            ConstructorInvoker<?> elementBaseInvoker = null;
            if (params.length > 0) {
                try {
                    Class<?>[] constructorParams = new Class[1 + params.length];
                    constructorParams[0] = clazz;
                    System.arraycopy(params, 0, constructorParams, 1, params.length);

                    elementBaseInvoker = Reflection.getConstructor(baseClass, constructorParams);
                } catch (Exception ignored) {
                }
            } else {
                try {
                    elementBaseInvoker = Reflection.getConstructor(baseClass, clazz);
                } catch (Exception ignored) {
                }
            }
            this.elementBaseInvoker = elementBaseInvoker;

            this.getterWithoutParams = () -> {
                try {
                    return this.baseInvoker != null ? this.baseInvoker.invoke() : baseClass.getDeclaredConstructor().newInstance();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return null;
                }
            };
        }
    }

    public Object create() {
        return getterWithoutParams.get();
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public int getTypeId() {
        return typeId;
    }

    public ConstructorInvoker<? extends NBTBase<?, ?>> getWrappedInvoker() {
        return wrappedInvoker;
    }

    public ConstructorInvoker<?> getBaseInvoker() {
        return baseInvoker;
    }

    public ConstructorInvoker<?> getElementBaseInvoker() {
        return elementBaseInvoker;
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

        for (Class<?> interfaceOfClass : clazz.getInterfaces()) {
            NBTType targetTypeFromLookup = typeLookup.get(interfaceOfClass);
            if (targetTypeFromLookup != null) {
                return targetTypeFromLookup;
            }
        }

        return UNKNOWN;
    }

}
