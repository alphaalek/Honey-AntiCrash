package me.alek.honey.netty.tag.nbt;

import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.converters.ApplicableConverter;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.structure.converters.MapConverter;
import me.alek.honey.netty.tag.ItemTag;
import me.alek.honey.netty.tag.nbt.array.AbstractArrayTag;
import me.alek.honey.netty.tag.nbt.array.ArrayTag;
import me.alek.honey.netty.tag.nbt.array.GeneticArrayTypes;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class CompoundTag extends AbstractTag<Map<String, Object>, MapConverter<String, Object, AbstractTag<?, ?, ?>>, CompoundTag> implements ItemTag {

    public static final CompoundTag EMPTY = new CompoundTag();
    private static final TagType TYPE = TagType.getFromCollection(TagType.Collection.COMPOUND);

    private MapConverter<String, Object, AbstractTag<?, ?, ?>> delegate;

    public CompoundTag(Object rawNms) {
        super(rawNms);
    }

    public CompoundTag() {
        super(TYPE.createRawDelegate());
    }

    public CompoundTag(Map<String, Object> map) {
        super(createBase());

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            getValue().putConverted(entry.getKey(), NBTFactory.createTag(entry.getValue()));
        }
    }

    private static Object createBase() {
        if (TYPE.equals(TagType.COMPOUND_LEGACY)) {
            return TYPE.createRawDelegate();
        } else if (TYPE.equals(TagType.COMPOUND_MODERN)) {
            return TYPE.getNMSInvoker().invoke(new HashMap<>());
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public Representation<CompoundTag> asNBT() {
        return Representation.as(this);
    }

    @Override
    public Function<ReflectStructure<Object, ?>, ReflectStructure<Map<String, Object>, MapConverter<String, Object, AbstractTag<?, ?, ?>>>> applyStructure(ApplicableConverter<Map<String, Object>, MapConverter<String, Object, AbstractTag<?, ?, ?>>> converter) {
        return (structure) -> structure.withSpecificType(Map.class, converter, false);
    }

    @Override
    public ApplicableConverter<Map<String, Object>, MapConverter<String, Object, AbstractTag<?, ?, ?>>> getNBTConverter() {
        if (this.delegate == null) {
            this.delegate = Converters.getNBTCompoundConverter();
        }
        return this.delegate;
    }

    public AbstractTag<?, ?, ?> remove(String key) {
        return getValue().removeConverted(key);
    }

    public void clear() {
        getValue().clear();
    }

    public int size() {
        return getValue().size();
    }

    public boolean isEmpty() {
        return getValue().isEmpty();
    }

    public Set<String> getKeys() {
        return getValue().keySet();
    }

    public Collection<AbstractTag<?, ?, ?>> getBases() {
        return getValue().convertedValues();
    }

    public AbstractTag<?, ?, ?> get(String key) {
        return getValue().getConverted(key);
    }

    public <T> T getPrimitive(String key) {
        return get(key).<T>asPrimitive().getValue();
    }

    public boolean hasKey(String key) {
        return getValue().containsKey(key);
    }

    public boolean hasKeyOfType(String key, byte typeId) {
        return hasKey(key) && get(key).getType().getTypeId() == typeId;
    }

    public boolean hasKeyOfType(String key, TagType type) {
        return hasKey(key) && get(key).getType().equals(type);
    }

    public void put(String key, AbstractTag<?, ?, ?> base) {
        getValue().putConverted(key, base);
    }

    public void putPrimitive(String key, TagType type, Object val) {
        getValue().putConverted(key, type.getWrapperInvoker().invoke(type, val));
    }

    public <IN> void putArray(String key, TagType of, Iterable<IN> array) {
        AbstractArrayTag<?, ?, ?> tag;
        if (of.equals(TagType.getFromCollection(TagType.Collection.ARRAY))) {
            tag = new GeneticArrayTypes.ArrayOfArrayTag<>(array);
        } else if (of.equals(TagType.getFromCollection(TagType.Collection.COMPOUND))) {
            tag = new GeneticArrayTypes.ArrayOfCompoundTag(array);
        } else {
            tag = new ArrayTag<>(of, array);
        }

        getValue().putConverted(key, tag);
    }

    public void putCompound(String key, Map<String, Object> map) {
        getValue().putConverted(key, new CompoundTag(map));
    }

    public void putIfAbsent(String key, AbstractTag<?, ?, ?> base) {
        if (!hasKey(key)) {
            put(key, base);
        }
    }

    @Override
    public void write(DataOutputStream output, TagVisitor<DataOutputStream> visitor) throws IOException {
        getType().<CompoundTag>getCodec().write(output, this, visitor);
    }

    @SuppressWarnings("unchecked")
    public <T> T getUnspecificDelegate(String key) {
        if (hasKey(key)) {
            return (T) get(key).getValue();
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractTag<?, ?, T>> T getUnspecificDeclaring(String key) {
        if (hasKey(key)) {
            return (T) get(key);
        }

        return null;
    }

    public <R extends AbstractTag<?, ?, R>, AT extends AbstractArrayTag<R, AT, ?>> AT getAbstractArrayOf(String key, TagType type) {
        if (type == TagType.getFromCollection(TagType.Collection.ARRAY) && !hasKeyOfType(key, TagType.getFromCollection(TagType.Collection.ARRAY_OF_ARRAY))) {
            return null;
        } else if (type == TagType.getFromCollection(TagType.Collection.COMPOUND) && !hasKeyOfType(key, TagType.getFromCollection(TagType.Collection.ARRAY_OF_COMPOUND))) {
            return null;
        } else if (type == TagType.getFromCollection(TagType.Collection.ARRAY_OF_COMPOUND) || type == TagType.getFromCollection(TagType.Collection.ARRAY_OF_ARRAY)) {
            throw new IllegalArgumentException("Array can not have nested array typeOf");
        } else if (!type.hasCollection() && !hasKeyOfType(key, TagType.getFromCollection(TagType.Collection.ARRAY))) {
            return null;
        }

        return getAbstractArray(key);
    }

    public <R extends AbstractTag<?, ?, R>, AT extends AbstractArrayTag<R, AT, ?>> AT getAbstractArrayOf(String key, Class<?> clazz) {
        return getAbstractArrayOf(key, TagType.getFromClass(clazz));
    }

    public <R> ArrayTag<R> getArrayOf(String key, Class<R> clazz) {
        return getAbstractArrayOf(key, clazz);
    }

    public <R> ArrayTag<R> getArrayOf(String key, TagType nbtType) {
        return getAbstractArrayOf(key, nbtType);
    }

    public GeneticArrayTypes.ArrayOfCompoundTag getArrayOfCompound(String key) {
        return getAbstractArrayOf(key, TagType.getFromCollection(TagType.Collection.COMPOUND));
    }

    public <ARRAY extends GeneticArrayTypes.ArrayOfArrayTag<ARRAY, IN, C>, IN extends AbstractArrayTag<C, IN, ?>, C extends AbstractTag<?, ?, C>> GeneticArrayTypes.ArrayOfArrayTag<ARRAY, IN, C> getArrayOfArray(String key) {
        return getAbstractArrayOf(key, TagType.getFromCollection(TagType.Collection.ARRAY));
    }

    public <T> ArrayTag<T> getArray(String key) {
        return getAbstractArray(key);
    }

    public <R extends AbstractTag<?, ?, R>, RT extends AbstractArrayTag<R, RT, ?>> RT getAbstractArray(String key) {
        return getUnspecificDeclaring(key);
    }

    public CompoundTag getCompound(String key) {
        return getUnspecificDeclaring(key);
    }

    public byte getByte(String key) {
        return getUnspecificDelegate(key);
    }

    public short getShort(String key) {
        return getUnspecificDelegate(key);
    }

    public int getInt(String key) {
        return getUnspecificDelegate(key);
    }

    public long getLong(String key) {
        return getUnspecificDelegate(key);
    }

    public float getFloat(String key) {
        return getUnspecificDelegate(key);
    }

    public double getDouble(String key) {
        return getUnspecificDelegate(key);
    }

    public byte[] getByteArray(String key) {
        return getUnspecificDelegate(key);
    }

    public String getString(String key) {
        return getUnspecificDelegate(key);
    }

    public int[] getIntArray(String key) {
        return getUnspecificDelegate(key);
    }

    public long[] getLongArray(String key) {
        return getUnspecificDelegate(key);
    }
}
