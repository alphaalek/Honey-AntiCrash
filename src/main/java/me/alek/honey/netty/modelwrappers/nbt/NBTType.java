package me.alek.honey.netty.modelwrappers.nbt;

import me.alek.honey.netty.modelwrappers.nbt.lists.NBTList;
import me.alek.honey.netty.modelwrappers.nbt.lists.NBTListOfCompound;
import me.alek.honey.netty.modelwrappers.nbt.lists.NBTListOfList;
import me.alek.honey.netty.utility.ConstructorInvoker;
import me.alek.honey.netty.utility.Reflection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public enum NBTType {
    END(0, void.class, getNBTClass("NBTTagEnd")),
    BYTE(1, byte.class, getNBTClass("NBTTagByte")),
    SHORT(2, short.class, getNBTClass("NBTTagShort")),
    INT(3, int.class, getNBTClass("NBTTagInt")),
    LONG(4, long.class, getNBTClass("NBTTagLong")),
    FLOAT(5, float.class, getNBTClass("NBTTagFloat")),
    DOUBLE(6, double.class, getNBTClass("NBTTagDouble")),
    BYTEARRAY(7, byte[].class, getNBTClass("NBTTagByteArray")),
    STRING(8, String.class, getNBTClass("NBTTagString")),
    @SuppressWarnings("unchecked")
    LIST(9, List.class, getNBTClass("NBTTagList"),
            new ConstructorInvoker<>(castNBTBase(NBTList.class), Object.class, NBTType.class, NBTRoot.class, NBTBase.class)),
    COMPOUND(10, Map.class, getNBTClass("NBTTagCompound"),
            new ConstructorInvoker<>(castNBTBase(NBTCompound.class), Object.class, NBTRoot.class, NBTBase.class)),
    INTARRAY(11, int[].class, getNBTClass("NBTTagIntArray")),
    LONGARRAY(12, long[].class, getNBTClass("NBTTagLongArray")),
    LISTOFCOMOUND(-1, List.class, getNBTClass("NBTTagList"),
            new ConstructorInvoker<>(castNBTBase(NBTListOfCompound.class), Object.class, NBTRoot.class, NBTBase.class)),
    LISTOFLIST(-1, List.class, getNBTClass("NBTTagList"),
            new ConstructorInvoker<>(castNBTBase(NBTListOfList.class), Object.class, NBTRoot.class, NBTBase.class)),
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
    NBTType(int typeId, Class<?> clazz, Class<?> baseClass) {
        this(typeId, clazz, baseClass, new ConstructorInvoker<>(castNBTBase(NBTElement.class), Object.class, NBTRoot.class, NBTBase.class));
    }

    NBTType(int typeId, Class<?> clazz, Class<?> baseClass, ConstructorInvoker<? extends NBTBase<?, ?>> constructorInvoker) {
        this.typeId = typeId;
        this.clazz = clazz;

        try {
            this.baseInvoker = Reflection.getConstructor(baseClass);
        } catch (Exception ex) {
            this.baseInvoker = null;
        }

        try {
            this.elementBaseInvoker = Reflection.getConstructor(baseClass, clazz);
        } catch (Exception ex) {
            this.elementBaseInvoker = null;
        }

        this.wrappedInvoker = constructorInvoker;

        this.getterWithoutParams = () -> {
            try {
                return baseInvoker != null ? baseInvoker.invoke() : baseClass.newInstance();
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        };
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
