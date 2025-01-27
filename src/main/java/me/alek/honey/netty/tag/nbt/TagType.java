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
    @NMSInvoker() END((byte) 0, CodecTypes.EndCodec.class, getNBTClass("NBTTagEnd"), void.class),
    @NMSInvoker(params = {byte.class}) BYTE((byte) 1, CodecTypes.ByteCodec.class, getNBTClass("NBTTagByte"), byte.class),
    @NMSInvoker(params = {short.class}) SHORT((byte) 2, CodecTypes.ShortCodec.class, getNBTClass("NBTTagShort"), short.class),
    @NMSInvoker(params = {int.class}) INT((byte) 3, CodecTypes.IntCodec.class, getNBTClass("NBTTagInt"), int.class),
    @NMSInvoker(params = {long.class}) LONG((byte) 4, CodecTypes.LongCodec.class, getNBTClass("NBTTagLong"), long.class),
    @NMSInvoker(params = {float.class}) FLOAT((byte) 5, CodecTypes.FloatCodec.class, getNBTClass("NBTTagFloat"), float.class),
    @NMSInvoker(params = {double.class}) DOUBLE((byte) 6, CodecTypes.DoubleCodec.class, getNBTClass("NBTTagDouble"), double.class),
    @NMSInvoker(params = {byte[].class}) ARRAY_OF_BYTE((byte) 7, CodecTypes.ByteArrayCodec.class, getNBTClass("NBTTagByteArray"), byte[].class),
    @NMSInvoker(params = {String.class}) STRING((byte) 8, CodecTypes.StringCodec.class, getNBTClass("NBTTagString"), String.class),
    @IdPrioritized @Collection.OfCollection(collection = Collection.ARRAY) @NetProtocol.To(to = NetProtocol.v1_16_5) @NMSInvoker()
    ARRAY_LEGACY((byte) 9, CodecTypes.ArrayCodec.class, getNBTClass("NBTTagList"),
            Reflection.getConstructor(getNBTBaseClass(ArrayTag.class), Object.class, TagType.class),
            Reflection.getConstructor(getNBTBaseClass(ArrayTag.class), TagType.class, Iterable.class),
            List.class),
    @IdPrioritized @Collection.OfCollection(collection = Collection.ARRAY) @NetProtocol.From(from = NetProtocol.v1_17) @NMSInvoker(params = {List.class, byte.class})
    ARRAY_MODERN((byte) 9, CodecTypes.ArrayCodec.class, getNBTClass("NBTTagList"),
            Reflection.getConstructor(getNBTBaseClass(ArrayTag.class), Object.class, TagType.class),
            Reflection.getConstructor(getNBTBaseClass(ArrayTag.class), TagType.class, Iterable.class),
            List.class),
    @Collection.OfCollection(collection = Collection.ARRAY_OF_COMPOUND) @NetProtocol.To(to = NetProtocol.v1_16_5) @NMSInvoker()
    ARRAY_OF_COMPOUND_LEGACY((byte) 9, CodecTypes.ArrayCodec.class, getNBTClass("NBTTagList"),
            Reflection.getConstructor(getNBTBaseClass(GeneticArrayTypes.ArrayOfCompoundTag.class), Object.class),
            Reflection.getConstructor(getNBTBaseClass(GeneticArrayTypes.ArrayOfCompoundTag.class), Iterable.class),
            List.class),
    @Collection.OfCollection(collection = Collection.ARRAY_OF_COMPOUND) @NetProtocol.From(from = NetProtocol.v1_17) @NMSInvoker(params = {List.class, byte.class})
    ARRAY_OF_COMPOUND_MODERN((byte) 9, CodecTypes.ArrayCodec.class, getNBTClass("NBTTagList"),
            Reflection.getConstructor(getNBTBaseClass(GeneticArrayTypes.ArrayOfCompoundTag.class), Object.class),
            Reflection.getConstructor(getNBTBaseClass(GeneticArrayTypes.ArrayOfCompoundTag.class), Iterable.class),
            List.class),
    @Collection.OfCollection(collection = Collection.ARRAY_OF_ARRAY) @NetProtocol.To(to = NetProtocol.v1_16_5) @NMSInvoker()
    ARRAY_OF_ARRAY_LEGACY((byte) 9, CodecTypes.ArrayCodec.class, getNBTClass("NBTTagList"),
            Reflection.getConstructor(getNBTBaseClass(GeneticArrayTypes.ArrayOfArrayTag.class), Object.class),
            Reflection.getConstructor(getNBTBaseClass(GeneticArrayTypes.ArrayOfArrayTag.class), Iterable.class),
            List.class),
    @Collection.OfCollection(collection = Collection.ARRAY_OF_ARRAY) @NetProtocol.From(from = NetProtocol.v1_17) @NMSInvoker(params = {List.class, byte.class})
    ARRAY_OF_ARRAY_MODERN((byte) 9, CodecTypes.ArrayCodec.class, getNBTClass("NBTTagList"),
            Reflection.getConstructor(getNBTBaseClass(GeneticArrayTypes.ArrayOfArrayTag.class), Object.class),
            Reflection.getConstructor(getNBTBaseClass(GeneticArrayTypes.ArrayOfArrayTag.class), Iterable.class),
            List.class),
    @Collection.OfCollection(collection = Collection.COMPOUND) @NetProtocol.To(to = NetProtocol.v1_16_5) @NMSInvoker()
    COMPOUND_LEGACY((byte) 10, CodecTypes.CompoundCodec.class, getNBTClass("NBTTagCompound"),
            Reflection.getConstructor(getNBTBaseClass(CompoundTag.class), Object.class),
            Reflection.getConstructor(getNBTBaseClass(CompoundTag.class), Map.class),
            Map.class),
    @Collection.OfCollection(collection = Collection.COMPOUND) @NetProtocol.From(from = NetProtocol.v1_17) @NMSInvoker(params = {Map.class})
    COMPOUND_MODERN((byte) 10, CodecTypes.CompoundCodec.class, getNBTClass("NBTTagCompound"),
            Reflection.getConstructor(getNBTBaseClass(CompoundTag.class), Object.class),
            Reflection.getConstructor(getNBTBaseClass(CompoundTag.class), Map.class),
            Map.class),
    @NMSInvoker(params = {int[].class})
    ARRAY_OF_INT((byte) 11, CodecTypes.IntArrayCodec.class, getNBTClass("NBTTagIntArray"), int[].class),
    @NetProtocol.From(from = NetProtocol.v1_12_2) @NMSInvoker(params = {long[].class})
    ARRAY_OF_LONG((byte) 12, CodecTypes.LongArrayCodec.class,
            NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_12_2) ? getNBTClass("NBTTagLongArray") : null, long[].class),;

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    private @interface NMSInvoker {

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
    private final ConstructorInvoker<? extends AbstractTag<?, ?, ?>> bridgeWrapperInvoker;
    private final ConstructorInvoker<? extends AbstractTag<?, ?, ?>> wrapperInvoker;
    private final TagFactory factory;
    private final NBTCodec<?> codec;
    private final ConstructorInvoker<?> nmsInvoker;

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


        for (TagType t : TagType.values()) {
            if (t.dataTypeClass.isAssignableFrom(clazz)) {
                return t;
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
        this(
                typeId,
                codec,
                delegateClass,
                Reflection.getConstructor(getNBTBaseClass(PrimitiveTag.class), Object.class),
                Reflection.getConstructor(getNBTBaseClass(PrimitiveTag.class), TagType.class, Object.class),
                dataTypeClass);
    }

    TagType(byte typeId,
            Class<?> codec,
            Class<?> delegateClass,
            ConstructorInvoker<? extends AbstractTag<?, ?, ?>> bridgeWrapperInvoker,
            ConstructorInvoker<? extends AbstractTag<?, ?, ?>> wrapperInvoker,
            Class<?> dataTypeClass) {
        this.typeId = typeId;
        this.deleteClass = delegateClass;
        this.dataTypeClass = dataTypeClass;

        if (!isValid()) {
            this.codec = null;
            this.bridgeWrapperInvoker = null;
            this.wrapperInvoker = null;
            this.nmsInvoker = null;
            this.factory = null;
            return;
        }

        try {
            this.codec = (NBTCodec<?>) codec.getDeclaredConstructor().newInstance();
            this.bridgeWrapperInvoker = bridgeWrapperInvoker;
            this.wrapperInvoker = wrapperInvoker;

            Field field = TagType.class.getField(this.name());
            if (field.isAnnotationPresent(NMSInvoker.class)) {
                NMSInvoker invoker = field.getAnnotation(NMSInvoker.class);
                this.nmsInvoker = Reflection.getConstructor(delegateClass, invoker.params());
            } else {
                this.nmsInvoker = null;
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

    public ConstructorInvoker<?> getNMSInvoker() {
        return this.nmsInvoker;
    }

    public ConstructorInvoker<? extends AbstractTag<?, ?, ?>> getBridgeWrapperInvoker() {
        return this.bridgeWrapperInvoker;
    }

    public ConstructorInvoker<? extends AbstractTag<?, ?, ?>> getWrapperInvoker() {
        return this.wrapperInvoker;
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractTag<?, ?, T>> NBTCodec<T> getCodec() {
        return (NBTCodec<T>) this.codec;
    }
}
