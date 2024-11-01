package me.alek.honey.netty.nbt;

import me.alek.honey.netty.nbt.array.GeneticArrayTypes;
import me.alek.honey.netty.nbt.array.NBTArray;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.ConstructorInvoker;
import me.alek.honey.util.reflection.Reflection;
import org.bukkit.Bukkit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public enum NBTType {
    @BaseInvoker() END(0, CodecTypes.EndCodec.class, getNBTClass("NBTTagEnd"), void.class),
    @BaseInvoker(params = {byte.class}) BYTE(1, CodecTypes.ByteCodec.class, getNBTClass("NBTTagByte"), byte.class),
    @BaseInvoker(params = {short.class}) SHORT(2, CodecTypes.ShortCodec.class, getNBTClass("NBTTagShort"), short.class),
    @BaseInvoker(params = {int.class}) INT(3, CodecTypes.IntCodec.class, getNBTClass("NBTTagInt"), int.class),
    @BaseInvoker(params = {long.class}) LONG(4, CodecTypes.LongCodec.class, getNBTClass("NBTTagLong"), long.class),
    @BaseInvoker(params = {float.class}) FLOAT(5, CodecTypes.FloatCodec.class, getNBTClass("NBTTagFloat"), float.class),
    @BaseInvoker(params = {double.class}) DOUBLE(6, CodecTypes.DoubleCodec.class, getNBTClass("NBTTagDouble"), double.class),
    @BaseInvoker(params = {byte[].class}) ARRAY_OF_BYTE(7, CodecTypes.ByteArrayCodec.class, getNBTClass("NBTTagByteArray"), byte[].class),
    @BaseInvoker(params = {String.class}) STRING(8, CodecTypes.StringCodec.class, getNBTClass("NBTTagString"), String.class),
    @BaseInvoker(params = {List.class, byte.class}) ARRAY(9, CodecTypes.ArrayCodec.class, getNBTClass("NBTTagList"),
            Reflection.getConstructor(getNBTBaseClass(NBTArray.class), Object.class, NBTType.class, NBTRoot.class, NBTBase.class, boolean.class), List.class),
    @BaseInvoker(params = {Map.class}) COMPOUND(10, CodecTypes.CompoundCodec.class, getNBTClass("NBTTagCompound"),
            Reflection.getConstructor(getNBTBaseClass(NBTCompound.class), Object.class, NBTRoot.class, NBTBase.class, boolean.class), Map.class),
    @BaseInvoker(params = {int[].class}) ARRAY_OF_INT(11, CodecTypes.IntArrayCodec.class, getNBTClass("NBTTagIntArray"), int[].class),
    @BaseInvoker(params = {long[].class}) ARRAY_OF_LONG(12, CodecTypes.LongArrayCodec.class,
            NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_12_2) ? getNBTClass("NBTTagLongArray") : null, long[].class),
    @BaseInvoker(params = {List.class, byte.class}) ARRAY_OF_COMPOUND(9, CodecTypes.ArrayCodec.class, getNBTClass("NBTTagList"),
            Reflection.getConstructor(getNBTBaseClass(GeneticArrayTypes.NBTArrayOfCompound.class), Object.class, NBTRoot.class, NBTBase.class, boolean.class), List.class),
    @BaseInvoker(params = {List.class, byte.class}) ARRAY_OF_ARRAY(9, CodecTypes.ArrayCodec.class, getNBTClass("NBTTagList"),
            Reflection.getConstructor(getNBTBaseClass(GeneticArrayTypes.NBTArrayOfArray.class), Object.class, NBTRoot.class, NBTBase.class, boolean.class), List.class)
    ;

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    private @interface BaseInvoker {

        Class<?>[] params() default {};
    }

    private final int typeId;
    private final Class<?> dataTypeClass;
    private final ConstructorInvoker<? extends NBTBase<?, ?>> wrappedInvoker;
    private final TagFactory factory;
    private final NBTCodec<?> codec;
    private final ConstructorInvoker<?> baseInvoker;

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

    NBTType(int typeId, Class<? extends NBTCodec<?>> codec, Class<?> delegateClass, Class<?> dataTypeClass) {
        this(typeId, codec, delegateClass, Reflection.getConstructor(getNBTBaseClass(NBTElement.class), Object.class, NBTRoot.class, NBTBase.class, boolean.class), dataTypeClass);
    }

    NBTType(int typeId, Class<? extends NBTCodec<?>> codec, Class<?> delegateClass, ConstructorInvoker<? extends NBTBase<?, ?>> wrappedInvoker, Class<?> dataTypeClass) {
        this.typeId = typeId;
        this.dataTypeClass = dataTypeClass;

        try {
            this.codec = codec.getDeclaredConstructor().newInstance();
            this.wrappedInvoker = wrappedInvoker;

            Field field = NBTType.class.getField(this.name());
            if (field.isAnnotationPresent(BaseInvoker.class)) {
                BaseInvoker invoker = field.getAnnotation(BaseInvoker.class);
                this.baseInvoker = Reflection.getConstructor(delegateClass, invoker.params());
            } else {
                this.baseInvoker = null;
            }

            TagFactory factory;
            try {
                factory = new TagFactory() {

                    final ConstructorInvoker<?> constructor = Reflection.getConstructor(delegateClass);

                    @Override
                    public Object create() throws Exception {
                        return constructor.invoke();
                    }
                };
            } catch (Exception ignored) {
                factory = null;
            }
            this.factory = factory;

        } catch (Exception ex) {
            throw new RuntimeException("Failed to create nbt", ex);
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

    public ConstructorInvoker<?> getBaseInvoker() {
        return this.baseInvoker;
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
