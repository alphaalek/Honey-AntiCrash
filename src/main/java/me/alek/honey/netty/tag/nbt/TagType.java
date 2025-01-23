package me.alek.honey.netty.tag.nbt;

import me.alek.honey.netty.tag.nbt.array.ArrayTag;
import me.alek.honey.netty.tag.nbt.array.GeneticArrayTypes;
import me.alek.honey.util.Array;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.protocol.NetProtocolRange;
import me.alek.honey.util.reflection.ConstructorInvoker;
import me.alek.honey.util.reflection.Reflection;
import org.bukkit.Bukkit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public enum TagType {
    @BaseInvoker() END((byte) 0, CodecTypes.EndCodec.class, getNBTClass("NBTTagEnd"), void.class),
    @BaseInvoker(params = {byte.class}) BYTE((byte) 1, CodecTypes.ByteCodec.class, getNBTClass("NBTTagByte"), byte.class),
    @BaseInvoker(params = {short.class}) SHORT((byte) 2, CodecTypes.ShortCodec.class, getNBTClass("NBTTagShort"), short.class),
    @BaseInvoker(params = {int.class}) INT((byte) 3, CodecTypes.IntCodec.class, getNBTClass("NBTTagInt"), int.class),
    @BaseInvoker(params = {long.class}) LONG((byte) 4, CodecTypes.LongCodec.class, getNBTClass("NBTTagLong"), long.class),
    @BaseInvoker(params = {float.class}) FLOAT((byte) 5, CodecTypes.FloatCodec.class, getNBTClass("NBTTagFloat"), float.class),
    @BaseInvoker(params = {double.class}) DOUBLE((byte) 6, CodecTypes.DoubleCodec.class, getNBTClass("NBTTagDouble"), double.class),
    @BaseInvoker(params = {byte[].class}) ARRAY_OF_BYTE((byte) 7, CodecTypes.ByteArrayCodec.class, getNBTClass("NBTTagByteArray"), byte[].class),
    @BaseInvoker(params = {String.class}) STRING((byte) 8, CodecTypes.StringCodec.class, getNBTClass("NBTTagString"), String.class),
    @IdPrioritized @Collection.OfCollection(collection = Collection.ARRAY) @NetProtocol.To(to = NetProtocol.v1_16_5) @BaseInvoker()
    ARRAY_LEGACY((byte) 9, CodecTypes.ArrayCodec.class, getNBTClass("NBTTagList"),
            Reflection.getConstructor(getNBTBaseClass(ArrayTag.class), Object.class, TagType.class, boolean.class), List.class),
    @IdPrioritized @Collection.OfCollection(collection = Collection.ARRAY) @NetProtocol.From(from = NetProtocol.v1_17) @BaseInvoker(params = {List.class, byte.class})
    ARRAY_MODERN((byte) 9, CodecTypes.ArrayCodec.class, getNBTClass("NBTTagList"),
            Reflection.getConstructor(getNBTBaseClass(ArrayTag.class), Object.class, TagType.class, boolean.class), List.class),
    @Collection.OfCollection(collection = Collection.ARRAY_OF_COMPOUND) @NetProtocol.To(to = NetProtocol.v1_16_5) @BaseInvoker()
    ARRAY_OF_COMPOUND_LEGACY((byte) 9, CodecTypes.ArrayCodec.class, getNBTClass("NBTTagList"),
            Reflection.getConstructor(getNBTBaseClass(GeneticArrayTypes.ArrayOfCompoundTag.class), Object.class, boolean.class), List.class),
    @Collection.OfCollection(collection = Collection.ARRAY_OF_COMPOUND) @NetProtocol.From(from = NetProtocol.v1_17) @BaseInvoker(params = {List.class, byte.class})
    ARRAY_OF_COMPOUND_MODERN((byte) 9, CodecTypes.ArrayCodec.class, getNBTClass("NBTTagList"),
            Reflection.getConstructor(getNBTBaseClass(GeneticArrayTypes.ArrayOfCompoundTag.class), Object.class, boolean.class), List.class),
    @Collection.OfCollection(collection = Collection.ARRAY_OF_ARRAY) @NetProtocol.To(to = NetProtocol.v1_16_5) @BaseInvoker()
    ARRAY_OF_ARRAY_LEGACY((byte) 9, CodecTypes.ArrayCodec.class, getNBTClass("NBTTagList"),
            Reflection.getConstructor(getNBTBaseClass(GeneticArrayTypes.ArrayOfArrayTag.class), Object.class, boolean.class), List.class),
    @Collection.OfCollection(collection = Collection.ARRAY_OF_ARRAY) @NetProtocol.From(from = NetProtocol.v1_17) @BaseInvoker(params = {List.class, byte.class})
    ARRAY_OF_ARRAY_MODERN((byte) 9, CodecTypes.ArrayCodec.class, getNBTClass("NBTTagList"),
            Reflection.getConstructor(getNBTBaseClass(GeneticArrayTypes.ArrayOfArrayTag.class), Object.class, boolean.class), List.class),
    @Collection.OfCollection(collection = Collection.COMPOUND) @NetProtocol.To(to = NetProtocol.v1_16_5) @BaseInvoker()
    COMPOUND_LEGACY((byte) 10, CodecTypes.CompoundCodec.class, getNBTClass("NBTTagCompound"),
            Reflection.getConstructor(getNBTBaseClass(CompoundTag.class), Object.class, boolean.class), Map.class),
    @Collection.OfCollection(collection = Collection.COMPOUND) @NetProtocol.From(from = NetProtocol.v1_17) @BaseInvoker(params = {Map.class})
    COMPOUND_MODERN((byte) 10, CodecTypes.CompoundCodec.class, getNBTClass("NBTTagCompound"),
            Reflection.getConstructor(getNBTBaseClass(CompoundTag.class), Object.class, boolean.class), Map.class),
    @BaseInvoker(params = {int[].class})
    ARRAY_OF_INT((byte) 11, CodecTypes.IntArrayCodec.class, getNBTClass("NBTTagIntArray"), int[].class),
    @NetProtocol.From(from = NetProtocol.v1_12_2) @BaseInvoker(params = {long[].class})
    ARRAY_OF_LONG((byte) 12, CodecTypes.LongArrayCodec.class,
            NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_12_2) ? getNBTClass("NBTTagLongArray") : null, long[].class),;

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    private @interface BaseInvoker {

        Class<?>[] params() default {};
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    private @interface IdPrioritized {

    }

    public enum Collection {
        COMPOUND,
        ARRAY,
        ARRAY_OF_COMPOUND,
        ARRAY_OF_ARRAY
        ;

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.FIELD)
        private @interface OfCollection {

            Collection collection();
        }
    }

    private interface TagFactory {

        Object create() throws Exception;
    }

    private final byte typeId;
    private final Class<?> dataTypeClass;
    private final Class<?> deleteClass;
    private final ConstructorInvoker<? extends AbstractTag<?, ?, ?>> wrappedInvoker;
    private final TagFactory factory;
    private final NBTCodec<?> codec;
    private final ConstructorInvoker<?> baseInvoker;

    public static TagType getFromTypeId(byte typeId) {
        try {
            return TYPE_ID_LOOKUP.get(typeId);
        } catch (Exception ex) {
            return null;
        }
    }

    public static TagType getFromClass(Class<?> clazz) {
        TagType type = TYPE_LOOKUP.get(clazz);

        if (type != null) {
            return type;
        }

        for (Class<?> interfaceOfClass : clazz.getInterfaces()) {
            TagType targetTypeFromLookup = TYPE_LOOKUP.get(interfaceOfClass);
            if (targetTypeFromLookup != null) {
                return targetTypeFromLookup;
            }
        }

        return null;
    }

    public static TagType getFromCollection(Collection collection) {
        return TYPE_COLLECTION_LOOKUP.get(collection);
    }

    private static final Map<Class<?>, TagType> TYPE_LOOKUP = new HashMap<>();
    private static final EnumMap<Collection, TagType> TYPE_COLLECTION_LOOKUP = new EnumMap<>(Collection.class);
    private static final Array<TagType> TYPE_ID_LOOKUP = new Array<>();

    private static Class<?> getNBTClass(String name) {
        return Reflection.getUnsureClass("{nms}." + name, "{nms}.nbt." + name);
    }

    static {
        for (TagType type : values()) {
            if (!type.isValid()) {
                continue;
            }

            if (TYPE_ID_LOOKUP.has(type.getTypeId())) {
                TagType at = TYPE_ID_LOOKUP.get(type.getTypeId());
                try {
                    Field fieldAt = TagType.class.getField(at.name());
                    if (!fieldAt.isAnnotationPresent(IdPrioritized.class)) {
                        TYPE_ID_LOOKUP.set(type.getTypeId(), type);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                TYPE_ID_LOOKUP.set(type.getTypeId(), type);
            }

            if (type.hasCollection()) {
                TYPE_COLLECTION_LOOKUP.put(type.getCollection(), type);
            }

            TYPE_LOOKUP.put(type.getDataTypeClass(), type);
        }

        if (TYPE_ID_LOOKUP.compact().size() != TYPE_ID_LOOKUP.size()) {
            throw new RuntimeException("Missing nbt type indices. " + TYPE_ID_LOOKUP);
        }
    }

    @SuppressWarnings("unchecked")
    public static Class<? extends AbstractTag<?, ?, ?>> getNBTBaseClass(Class<?> clazz) {
        try {
            if (AbstractTag.class.isAssignableFrom(clazz)) {
                return (Class<? extends AbstractTag<?, ?, ?>>) clazz.asSubclass(AbstractTag.class);
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    public boolean isValid() {
        try {
            Field field = TagType.class.getField(this.name());
            NetProtocol.From from = field.getAnnotation(NetProtocol.From.class);
            NetProtocol.To to = field.getAnnotation(NetProtocol.To.class);

            return NetProtocolRange.matchProtocol(to, from);

        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean hasCollection() {
        try {
            Field field = TagType.class.getField(this.name());
            return field.isAnnotationPresent(Collection.OfCollection.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public Collection getCollection() {
        try {
            Field field = TagType.class.getField(this.name());
            Collection.OfCollection ofCollection = field.getAnnotation(Collection.OfCollection.class);
            if (ofCollection != null) {
                return ofCollection.collection();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    TagType(byte typeId, Class<?> codec, Class<?> delegateClass, Class<?> dataTypeClass) {
        this(typeId, codec, delegateClass, Reflection.getConstructor(getNBTBaseClass(PrimitiveTag.class), Object.class, boolean.class), dataTypeClass);
    }

    TagType(byte typeId, Class<?> codec, Class<?> delegateClass, ConstructorInvoker<? extends AbstractTag<?, ?, ?>> wrappedInvoker, Class<?> dataTypeClass) {
        this.typeId = typeId;
        this.deleteClass = delegateClass;
        this.dataTypeClass = dataTypeClass;

        if (!isValid()) {
            this.codec = null;
            this.wrappedInvoker = null;
            this.baseInvoker = null;
            this.factory = null;
            return;
        }

        try {
            this.codec = (NBTCodec<?>) codec.getDeclaredConstructor().newInstance();
            this.wrappedInvoker = wrappedInvoker;

            Field field = TagType.class.getField(this.name());
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
                        return constructor.invokeWithException();
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

    public Class<?> getDelegateClass() {
        return this.deleteClass;
    }

    public byte getTypeId() {
        return this.typeId;
    }

    public ConstructorInvoker<? extends AbstractTag<?, ?, ?>> getWrappedInvoker() {
        return this.wrappedInvoker;
    }

    public ConstructorInvoker<?> getBaseInvoker() {
        return this.baseInvoker;
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractTag<?, ?, T>> NBTCodec<T> getCodec() {
        return (NBTCodec<T>) this.codec;
    }
}
