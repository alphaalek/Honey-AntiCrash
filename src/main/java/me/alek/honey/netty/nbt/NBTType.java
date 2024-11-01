package me.alek.honey.netty.nbt;

import me.alek.honey.netty.nbt.array.GeneticArrayTypes;
import me.alek.honey.netty.nbt.array.NBTArray;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.ConstructorInvoker;
import me.alek.honey.util.reflection.Reflection;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public enum NBTType {
    END(0, CodecTypes.EndCodec.class, getNBTClass("NBTTagEnd"), void.class),
    BYTE(1, CodecTypes.ByteCodec.class, getNBTClass("NBTTagByte"), byte.class),
    SHORT(2, CodecTypes.ShortCodec.class, getNBTClass("NBTTagShort"), short.class),
    INT(3, CodecTypes.IntCodec.class, getNBTClass("NBTTagInt"), int.class),
    LONG(4, CodecTypes.LongCodec.class, getNBTClass("NBTTagLong"), long.class),
    FLOAT(5, CodecTypes.FloatCodec.class, getNBTClass("NBTTagFloat"), float.class),
    DOUBLE(6, CodecTypes.DoubleCodec.class, getNBTClass("NBTTagDouble"), double.class),
    ARRAY_OF_BYTE(7, CodecTypes.ByteArrayCodec.class, getNBTClass("NBTTagByteArray"), byte[].class),
    STRING(8, CodecTypes.StringCodec.class, getNBTClass("NBTTagString"), String.class),
    @SuppressWarnings("unchecked")
    ARRAY(9, (Class<? extends NBTCodec<?>>) CodecTypes.ArrayCodec.class, getNBTClass("NBTTagList"),
            Reflection.getConstructor(getNBTBaseClass(NBTArray.class), Object.class, NBTType.class, NBTRoot.class, NBTBase.class, boolean.class), List.class, byte.class),
    COMPOUND(10, CodecTypes.CompoundCodec.class, getNBTClass("NBTTagCompound"),
            Reflection.getConstructor(getNBTBaseClass(NBTCompound.class), Object.class, NBTRoot.class, NBTBase.class, boolean.class), Map.class),
    ARRAY_OF_INT(11, CodecTypes.IntArrayCodec.class, getNBTClass("NBTTagIntArray"), int[].class),
    ARRAY_OF_LONG(12, CodecTypes.LongArrayCodec.class,
            NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_12_2) ? getNBTClass("NBTTagLongArray") : null, long[].class),
    @SuppressWarnings("unchecked")
    ARRAY_OF_COMPOUND(9, (Class<? extends NBTCodec<?>>) CodecTypes.ArrayCodec.class, getNBTClass("NBTTagList"),
            Reflection.getConstructor(getNBTBaseClass(GeneticArrayTypes.NBTArrayOfCompound.class), Object.class, NBTRoot.class, NBTBase.class, boolean.class), List.class, byte.class),
    @SuppressWarnings("unchecked")
    ARRAY_OF_ARRAY(9, (Class<? extends NBTCodec<?>>) CodecTypes.ArrayCodec.class, getNBTClass("NBTTagList"),
            Reflection.getConstructor(getNBTBaseClass(GeneticArrayTypes.NBTArrayOfArray.class), Object.class, NBTRoot.class, NBTBase.class, boolean.class), List.class, byte.class)
    ;

    private final int typeId;
    private final Class<?> dataTypeClass;
    private final ConstructorInvoker<? extends NBTBase<?, ?>> wrappedInvoker;
    private final ConstructorInvoker<?> elementBaseInvoker;
    private final TagFactory factory;
    private final NBTCodec<?> codec;

    private interface TagFactory {

        Object create() throws Exception;
    }

    private static final Map<Class<?>, NBTType> TYPE_LOOKUP = new HashMap<>();

    private static Class<?> getNBTClass(String name) {
        return Reflection.getUnsureClass("{nms}." + name, "{nms}.nbt." + name);
    }

    static {
        for (NBTType nbtType : values()) {
            if (TYPE_LOOKUP.containsKey(nbtType.getDataTypeClass())) {
                continue;
            }

            TYPE_LOOKUP.put(nbtType.getDataTypeClass(), nbtType);
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

    NBTType(int typeId, Class<? extends NBTCodec<?>> codec, Class<?> delegateClass, Class<?> clazz) {
        this(typeId, codec, delegateClass, Reflection.getConstructor(getNBTBaseClass(NBTElement.class), Object.class, NBTRoot.class, NBTBase.class, boolean.class), clazz);
    }

    NBTType(int typeId, Class<? extends NBTCodec<?>> codec, Class<?> delegateClass, ConstructorInvoker<? extends NBTBase<?, ?>> wrappedInvoker, Class<?> dataTypeClass, Class<?>... params) {
        this.typeId = typeId;
        this.dataTypeClass = dataTypeClass;

        try {
            this.codec = codec.getDeclaredConstructor().newInstance();
            this.wrappedInvoker = wrappedInvoker;

            Class<?>[] constructorParams = new Class[1 + params.length];
            constructorParams[0] = dataTypeClass;
            if (params.length > 0) {
                System.arraycopy(params, 0, constructorParams, 1, params.length);
            }
            this.elementBaseInvoker = Reflection.getConstructor(delegateClass, constructorParams);

            this.factory = new TagFactory() {

                final ConstructorInvoker<?> constructor = Reflection.getConstructor(delegateClass);

                @Override
                public Object create() throws Exception {
                    return constructor.invoke();
                }
            };
        } catch (Exception ex) {
            throw new RuntimeException("Failed to create nbt");
        }
    }

    public Object createRawDelegate() {
        try {
            return this.factory.create();
        } catch (Exception ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to create raw delegate for type " + this, ex);
            return null;
        }
    }

    public Class<?> getDataTypeClass() {
        return this.dataTypeClass;
    }

    public int getTypeId() {
        return this.typeId;
    }

    public ConstructorInvoker<? extends NBTBase<?, ?>> getWrappedInvoker() {
        return this.wrappedInvoker;
    }

    public ConstructorInvoker<?> getElementBaseInvoker() {
        return this.elementBaseInvoker;
    }

    @SuppressWarnings("unchecked")
    public <T extends NBTBase<?, ?>> NBTCodec<T> getCodec() {
        return (NBTCodec<T>) this.codec;
    }

    public static NBTType getFromTypeId(byte typeId) {
        try {
            return values()[typeId];
        } catch (Exception ex) {
            return null;
        }
    }

    public static NBTType getFromClass(Class<?> clazz) {
        NBTType type = TYPE_LOOKUP.get(clazz);

        if (type != null) {
            return type;
        }

        for (Class<?> interfaceOfClass : clazz.getInterfaces()) {
            NBTType targetTypeFromLookup = TYPE_LOOKUP.get(interfaceOfClass);
            if (targetTypeFromLookup != null) {
                return targetTypeFromLookup;
            }
        }

        return null;
    }
}
