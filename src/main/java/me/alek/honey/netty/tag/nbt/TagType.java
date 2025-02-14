package me.alek.honey.netty.tag.nbt;

import me.alek.honey.util.Array;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.protocol.NetProtocolRange;
import me.alek.honey.util.reflection.ConstructorInvoker;
import me.alek.honey.util.reflection.Reflection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.*;

public enum TagType {
    @InternalInvoker() END((byte) 0, CodecTypes.EndCodec.class, getNBTClass("NBTTagEnd"), void.class),
    @InternalInvoker(params = {byte.class}) BYTE((byte) 1, CodecTypes.ByteCodec.class, getNBTClass("NBTTagByte"), byte.class),
    @InternalInvoker(params = {short.class}) SHORT((byte) 2, CodecTypes.ShortCodec.class, getNBTClass("NBTTagShort"), short.class),
    @InternalInvoker(params = {int.class}) INT((byte) 3, CodecTypes.IntCodec.class, getNBTClass("NBTTagInt"), int.class),
    @InternalInvoker(params = {long.class}) LONG((byte) 4, CodecTypes.LongCodec.class, getNBTClass("NBTTagLong"), long.class),
    @InternalInvoker(params = {float.class}) FLOAT((byte) 5, CodecTypes.FloatCodec.class, getNBTClass("NBTTagFloat"), float.class),
    @InternalInvoker(params = {double.class}) DOUBLE((byte) 6, CodecTypes.DoubleCodec.class, getNBTClass("NBTTagDouble"), double.class),
    @InternalInvoker(params = {byte[].class}) ARRAY_OF_BYTE((byte) 7, CodecTypes.ByteArrayCodec.class, getNBTClass("NBTTagByteArray"), byte[].class),
    @InternalInvoker(params = {String.class}) STRING((byte) 8, CodecTypes.StringCodec.class, getNBTClass("NBTTagString"), String.class),
    @IdPrioritized @TagTypeCollection.OfCollection(collection = TagTypeCollection.ARRAY) @NetProtocol.To(to = NetProtocol.v1_14_4) @InternalInvoker()
    ARRAY_LEGACY((byte) 9, CodecTypes.ArrayCodec.class, getNBTClass("NBTTagList"), List.class),
    @IdPrioritized @TagTypeCollection.OfCollection(collection = TagTypeCollection.ARRAY) @NetProtocol.From(from = NetProtocol.v1_15) @InternalInvoker(params = {List.class, byte.class})
    ARRAY_MODERN((byte) 9, CodecTypes.ArrayCodec.class, getNBTClass("NBTTagList"), List.class),
    @TagTypeCollection.OfCollection(collection = TagTypeCollection.COMPOUND) @NetProtocol.To(to = NetProtocol.v1_14_4) @InternalInvoker()
    COMPOUND_LEGACY((byte) 10, CodecTypes.CompoundCodec.class, getNBTClass("NBTTagCompound"), Map.class),
    @TagTypeCollection.OfCollection(collection = TagTypeCollection.COMPOUND) @NetProtocol.From(from = NetProtocol.v1_15) @InternalInvoker(params = {Map.class})
    COMPOUND_MODERN((byte) 10, CodecTypes.CompoundCodec.class, getNBTClass("NBTTagCompound"), Map.class),
    @InternalInvoker(params = {int[].class})
    ARRAY_OF_INT((byte) 11, CodecTypes.IntArrayCodec.class, getNBTClass("NBTTagIntArray"), int[].class),
    @NetProtocol.From(from = NetProtocol.v1_12_2) @InternalInvoker(params = {long[].class})
    ARRAY_OF_LONG((byte) 12, CodecTypes.LongArrayCodec.class,
            NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_12_2) ? getNBTClass("NBTTagLongArray") : null, long[].class),
    ;

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    private @interface InternalInvoker {

        Class<?>[] params() default {};
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    private @interface IdPrioritized {

    }

    private final byte typeId;
    private final Class<?> dataTypeClass;
    private final Class<?> deleteClass;
    private final NBTCodec<?> codec;
    private final ConstructorInvoker<?> internalInvoker;

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

    public static TagType getFromInternalClass(Class<?> clazz) {
        return INTERNAL_TYPE_LOOKUP.get(clazz);
    }

    private static final Map<Class<?>, TagType> TYPE_LOOKUP = new HashMap<>();
    private static final Map<Class<?>, TagType> INTERNAL_TYPE_LOOKUP = new HashMap<>();
    private static final Array<TagType> TYPE_ID_LOOKUP = new Array<>();

    private static Class<?> getNBTClass(String name) {
        return Reflection.getUnsureClass("{nms}." + name, "{nms}.nbt." + name);
    }

    public static void load() {
        for (TagType type : values()) {
            if (!type.isValid()) {
                continue;
            }

            if (TYPE_ID_LOOKUP.has(type.getId())) {
                TagType at = TYPE_ID_LOOKUP.get(type.getId());
                try {
                    Field fieldAt = TagType.class.getField(at.name());
                    if (!fieldAt.isAnnotationPresent(IdPrioritized.class)) {
                        TYPE_ID_LOOKUP.set(type.getId(), type);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                TYPE_ID_LOOKUP.set(type.getId(), type);
            }

            INTERNAL_TYPE_LOOKUP.put(type.getInternalClass(), type);
            TYPE_LOOKUP.put(type.getDataTypeClass(), type);
        }

        if (TYPE_ID_LOOKUP.compact().size() != TYPE_ID_LOOKUP.size()) {
            throw new RuntimeException("Missing nbt type indices. " + TYPE_ID_LOOKUP);
        }

        TagTypeCollection.load();
    }

    public boolean isValid() {
        try {
            Field field = TagType.class.getField(this.name());
            NetProtocol.From from = field.getAnnotation(NetProtocol.From.class);
            NetProtocol.To to = field.getAnnotation(NetProtocol.To.class);

            return NetProtocolRange.matchProtocol(to, from);
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean hasCollection() {
        try {
            Field field = TagType.class.getField(this.name());
            return field.isAnnotationPresent(TagTypeCollection.OfCollection.class);
        } catch (Exception ignored) {
            return false;
        }
    }

    public TagTypeCollection getCollection() {
        try {
            Field field = TagType.class.getField(this.name());
            TagTypeCollection.OfCollection ofCollection = field.getAnnotation(TagTypeCollection.OfCollection.class);
            if (ofCollection != null) {
                return ofCollection.collection();
            } else {
                return null;
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    TagType(byte typeId,
            Class<?> codec,
            Class<?> internalClass,
            Class<?> dataTypeClass) {
        this.typeId = typeId;
        this.deleteClass = internalClass;
        this.dataTypeClass = dataTypeClass;

        if (!isValid()) {
            this.codec = null;
            this.internalInvoker = null;
            return;
        }

        try {
            this.codec = (NBTCodec<?>) codec.getDeclaredConstructor().newInstance();

            Field field = TagType.class.getField(this.name());
            if (field.isAnnotationPresent(InternalInvoker.class)) {
                InternalInvoker invoker = field.getAnnotation(InternalInvoker.class);
                this.internalInvoker = Reflection.getConstructor(internalClass, invoker.params());
            } else {
                throw new RuntimeException("Invalid tag type");
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to initialize tag type", ex);
        }
    }

    public byte getId() {
        return this.typeId;
    }

    public Class<?> getDataTypeClass() {
        return this.dataTypeClass;
    }

    public Class<?> getInternalClass() {
        return this.deleteClass;
    }

    public ConstructorInvoker<?> getInternalInvoker() {
        return this.internalInvoker;
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractTag<?, T>> NBTCodec<T> getCodec() {
        return (NBTCodec<T>) this.codec;
    }
}
