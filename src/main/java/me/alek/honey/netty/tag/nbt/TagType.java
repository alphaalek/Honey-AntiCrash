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
import java.util.*;

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
    @IdPrioritized @TagTypeCollection.OfCollection(collection = TagTypeCollection.ARRAY) @NetProtocol.To(to = NetProtocol.v1_16_5) @NMSInvoker()
    ARRAY_LEGACY((byte) 9, CodecTypes.ArrayCodec.class, getNBTClass("NBTTagList"),
            Reflection.getConstructor(getTagClass(ArrayTag.class), Object.class, TagType.class),
            Reflection.getConstructor(getTagClass(ArrayTag.class), TagType.class, Iterable.class),
            List.class),
    @IdPrioritized @TagTypeCollection.OfCollection(collection = TagTypeCollection.ARRAY) @NetProtocol.From(from = NetProtocol.v1_17) @NMSInvoker(params = {List.class, byte.class})
    ARRAY_MODERN((byte) 9, CodecTypes.ArrayCodec.class, getNBTClass("NBTTagList"),
            Reflection.getConstructor(getTagClass(ArrayTag.class), Object.class, TagType.class),
            Reflection.getConstructor(getTagClass(ArrayTag.class), TagType.class, Iterable.class),
            List.class),
    @TagTypeCollection.OfCollection(collection = TagTypeCollection.ARRAY_OF_COMPOUND) @NetProtocol.To(to = NetProtocol.v1_16_5) @NMSInvoker()
    ARRAY_OF_COMPOUND_LEGACY((byte) 9, CodecTypes.ArrayCodec.class, getNBTClass("NBTTagList"),
            Reflection.getConstructor(getTagClass(GeneticArrayTypes.ArrayOfCompoundTag.class), Object.class),
            Reflection.getConstructor(getTagClass(GeneticArrayTypes.ArrayOfCompoundTag.class), Iterable.class),
            List.class),
    @TagTypeCollection.OfCollection(collection = TagTypeCollection.ARRAY_OF_COMPOUND) @NetProtocol.From(from = NetProtocol.v1_17) @NMSInvoker(params = {List.class, byte.class})
    ARRAY_OF_COMPOUND_MODERN((byte) 9, CodecTypes.ArrayCodec.class, getNBTClass("NBTTagList"),
            Reflection.getConstructor(getTagClass(GeneticArrayTypes.ArrayOfCompoundTag.class), Object.class),
            Reflection.getConstructor(getTagClass(GeneticArrayTypes.ArrayOfCompoundTag.class), Iterable.class),
            List.class),
    @TagTypeCollection.OfCollection(collection = TagTypeCollection.ARRAY_OF_ARRAY) @NetProtocol.To(to = NetProtocol.v1_16_5) @NMSInvoker()
    ARRAY_OF_ARRAY_LEGACY((byte) 9, CodecTypes.ArrayCodec.class, getNBTClass("NBTTagList"),
            Reflection.getConstructor(getTagClass(GeneticArrayTypes.ArrayOfArrayTag.class), Object.class),
            Reflection.getConstructor(getTagClass(GeneticArrayTypes.ArrayOfArrayTag.class), Iterable.class),
            List.class),
    @TagTypeCollection.OfCollection(collection = TagTypeCollection.ARRAY_OF_ARRAY) @NetProtocol.From(from = NetProtocol.v1_17) @NMSInvoker(params = {List.class, byte.class})
    ARRAY_OF_ARRAY_MODERN((byte) 9, CodecTypes.ArrayCodec.class, getNBTClass("NBTTagList"),
            Reflection.getConstructor(getTagClass(GeneticArrayTypes.ArrayOfArrayTag.class), Object.class),
            Reflection.getConstructor(getTagClass(GeneticArrayTypes.ArrayOfArrayTag.class), Iterable.class),
            List.class),
    @TagTypeCollection.OfCollection(collection = TagTypeCollection.COMPOUND) @NetProtocol.To(to = NetProtocol.v1_16_5) @NMSInvoker()
    COMPOUND_LEGACY((byte) 10, CodecTypes.CompoundCodec.class, getNBTClass("NBTTagCompound"),
            Reflection.getConstructor(getTagClass(CompoundTag.class), Object.class),
            Reflection.getConstructor(getTagClass(CompoundTag.class), Map.class),
            Map.class),
    @TagTypeCollection.OfCollection(collection = TagTypeCollection.COMPOUND) @NetProtocol.From(from = NetProtocol.v1_17) @NMSInvoker(params = {Map.class})
    COMPOUND_MODERN((byte) 10, CodecTypes.CompoundCodec.class, getNBTClass("NBTTagCompound"),
            Reflection.getConstructor(getTagClass(CompoundTag.class), Object.class),
            Reflection.getConstructor(getTagClass(CompoundTag.class), Map.class),
            Map.class),
    @NMSInvoker(params = {int[].class})
    ARRAY_OF_INT((byte) 11, CodecTypes.IntArrayCodec.class, getNBTClass("NBTTagIntArray"), int[].class),
    @NetProtocol.From(from = NetProtocol.v1_12_2) @NMSInvoker(params = {long[].class})
    ARRAY_OF_LONG((byte) 12, CodecTypes.LongArrayCodec.class,
            NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_12_2) ? getNBTClass("NBTTagLongArray") : null, long[].class),
    ;

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    private @interface NMSInvoker {

        Class<?>[] params() default {};
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    private @interface IdPrioritized {

    }

    private final byte typeId;
    private final Class<?> dataTypeClass;
    private final Class<?> deleteClass;
    private final ConstructorInvoker<? extends AbstractTag<?, ?, ?>> bridgeWrapperInvoker;
    private final ConstructorInvoker<? extends AbstractTag<?, ?, ?>> wrapperInvoker;
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

    private static final Map<Class<?>, TagType> TYPE_LOOKUP = new HashMap<>();
    private static final Array<TagType> TYPE_ID_LOOKUP = new Array<>();

    private static Class<?> getNBTClass(String name) {
        return Reflection.getUnsureClass("{nms}." + name, "{nms}.nbt." + name);
    }

    static {
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

            TYPE_LOOKUP.put(type.getDataTypeClass(), type);
        }

        if (TYPE_ID_LOOKUP.compact().size() != TYPE_ID_LOOKUP.size()) {
            throw new RuntimeException("Missing nbt type indices. " + TYPE_ID_LOOKUP);
        }

        TagTypeCollection.load();
    }

    @SuppressWarnings("unchecked")
    public static Class<? extends AbstractTag<?, ?, ?>> getTagClass(Class<?> clazz) {
        try {
            if (AbstractTag.class.isAssignableFrom(clazz)) {
                return (Class<? extends AbstractTag<?, ?, ?>>) clazz.asSubclass(AbstractTag.class);
            } else {
                return null;
            }
        } catch (Exception ignored) {
            return null;
        }
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

    TagType(byte typeId, Class<?> codec, Class<?> delegateClass, Class<?> dataTypeClass) {
        this(
                typeId,
                codec,
                delegateClass,
                Reflection.getConstructor(getTagClass(PrimitiveTag.class), Object.class),
                Reflection.getConstructor(getTagClass(PrimitiveTag.class), TagType.class, Object.class),
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
        } catch (Exception ex) {
            throw new RuntimeException("Failed to create nbt", ex);
        }
    }

    public Class<?> getDataTypeClass() {
        return this.dataTypeClass;
    }

    public Class<?> getDelegateClass() {
        return this.deleteClass;
    }

    public byte getId() {
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
