package me.alek.honey.netty.modelwrappers.nbt;

import me.alek.honey.netty.modelwrappers.nbt.lists.NBTList;
import me.alek.honey.netty.modelwrappers.nbt.lists.NBTListOfCompound;
import me.alek.honey.netty.modelwrappers.nbt.lists.NBTListOfList;
import me.alek.honey.netty.util.ConstructorInvoker;
import me.alek.honey.netty.util.NetProtocol;
import me.alek.honey.netty.util.Reflection;

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
    BYTEARRAY(7, getNBTClass("NBTTagByteArray"), byte[].class),
    STRING(8, getNBTClass("NBTTagString"), String.class),
    @SuppressWarnings("unchecked")
    LIST(9, getNBTClass("NBTTagList"),
            new ConstructorInvoker<>(getNBTBaseClass(NBTList.class), Object.class, NBTType.class, NBTRoot.class, NBTBase.class, boolean.class), List.class, byte.class),
    COMPOUND(10, getNBTClass("NBTTagCompound"),
            new ConstructorInvoker<>(getNBTBaseClass(NBTCompound.class), Object.class, NBTRoot.class, NBTBase.class, boolean.class), Map.class, boolean.class),
    INTARRAY(11, getNBTClass("NBTTagIntArray"), int[].class),
    LONGARRAY(12, NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_12_2) ? getNBTClass("NBTTagLongArray") : null, long[].class),
    LISTOFCOMOUND(-1, getNBTClass("NBTTagList"),
            new ConstructorInvoker<>(getNBTBaseClass(NBTListOfCompound.class), Object.class, NBTRoot.class, NBTBase.class, boolean.class), List.class, byte.class),
    LISTOFLIST(-1, getNBTClass("NBTTagList"),
            new ConstructorInvoker<>(getNBTBaseClass(NBTListOfList.class), Object.class, NBTRoot.class, NBTBase.class, boolean.class), List.class, byte.class),
    UNKNOWN(-1, null, null);

    private final int typeId;
    private final Class<?> clazz;
    private ConstructorInvoker<?> baseInvoker;
    private ConstructorInvoker<?> elementBaseInvoker;
    private final ConstructorInvoker<? extends NBTBase<?, ?>> wrappedInvoker;
    private final Supplier<Object> getterWithoutParams;

    private static final Map<Class<?>, NBTType> typeLookup;

    private static Class<?> getNBTClass(String name) {
        return Reflection.getFuzzyClass("{nms}." + name, "{nms}.nbt." + name);
    }

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
    public static Class<? extends NBTBase<?, ?>> getNBTBaseClass(Class<?> clazz) {
        try {
            if (NBTBase.class.isAssignableFrom(clazz)) {
                return (Class<? extends NBTBase<?, ?>>) clazz.asSubclass(NBTBase.class);
            }
        } catch (Exception ex) {
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    NBTType(int typeId, Class<?> baseClass, Class<?> clazz) {
        this(typeId, baseClass, new ConstructorInvoker<>(getNBTBaseClass(NBTElement.class), Object.class, NBTRoot.class, NBTBase.class, boolean.class), clazz);
    }

    NBTType(int typeId, Class<?> baseClass, ConstructorInvoker<? extends NBTBase<?, ?>> constructorInvoker, Class<?> clazz, Class<?>... params) {
        this.typeId = typeId;
        this.clazz = clazz;

        if (baseClass == null) {
            this.baseInvoker = null;
            this.wrappedInvoker = null;
            this.getterWithoutParams = null;

        } else {
            this.wrappedInvoker = constructorInvoker;
            try {
                this.baseInvoker = Reflection.getConstructor(baseClass);
            } catch (Throwable ex) {
                this.baseInvoker = null;
            }

            if (params.length > 0) {
                try {
                    Class<?>[] constructorParams = new Class[1 + params.length];
                    constructorParams[0] = clazz;
                    System.arraycopy(params, 0, constructorParams, 1, params.length);

                    this.elementBaseInvoker = Reflection.getConstructor(baseClass, constructorParams);
                } catch (Throwable ex) {
                    this.elementBaseInvoker = null;
                }
            }

            this.getterWithoutParams = () -> {
                try {
                    return baseInvoker != null ? baseInvoker.invoke() : baseClass.newInstance();
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

        for (Class<?> interfaceClass : clazz.getInterfaces()) {
            NBTType interfaceTargetType = typeLookup.get(interfaceClass);
            if (interfaceTargetType != null) {
                return interfaceTargetType;
            }
        }

        return UNKNOWN;
    }

}
