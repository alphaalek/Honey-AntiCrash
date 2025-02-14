package me.alek.honey.netty.tag.nbt;

import me.alek.honey.netty.tag.ItemTag;
import me.alek.honey.util.LazyInit;

import java.util.*;
import java.util.stream.Collectors;

public class CompoundTag extends AbstractTag<Map<String, AbstractTag<?, ?>>, CompoundTag> implements ItemTag {

    public static final LazyInit<TagType> TYPE = new LazyInit<>(() -> TagTypeCollection.getFromCollection(TagTypeCollection.COMPOUND));
    private static final Map<String, Object> EMPTY_MAP = new HashMap<>();
    public static final CompoundTag EMPTY = new CompoundTag();

    private final Map<String, AbstractTag<?, ?>> delegate = new HashMap<>();

    public CompoundTag() {
        this(EMPTY_MAP);
    }

    public CompoundTag(Map<?, ?> map) {
        super(TYPE.get());

        @SuppressWarnings("unchecked")
        Map<String, Object> mapCast = (Map<String, Object>) map;
        for (Map.Entry<String, Object> entry : mapCast.entrySet()) {
            AbstractTag<?, ?> tag = NBTFactory.createTag(entry.getValue());
            if (tag != null) {
                this.delegate.put(entry.getKey(), tag);
            }
        }
    }

    @Override
    public Representation<CompoundTag> asNBT() {
        return Representation.as(this);
    }

    @Override
    public String serializeJson(String emptyKey) {
        String inner = this.delegate.entrySet().stream()
                .map(entry -> "\"" + (entry.getKey().isEmpty() ? emptyKey : entry.getKey()) + "\":" + entry.getValue().serializeJson(emptyKey))
                .collect(Collectors.joining(","));
        return "{" + inner + "}";
    }

    public Set<Map.Entry<String, AbstractTag<?, ?>>> entrySet() {
        return this.delegate.entrySet();
    }

    public Map<String, AbstractTag<?, ?>> getDelegate() {
        return this.delegate;
    }

    public AbstractTag<?, ?> remove(String key) {
        return this.delegate.remove(key);
    }

    public void clear() {
        this.delegate.clear();
        ;
    }

    public int size() {
        return this.delegate.size();
    }

    public boolean isEmpty() {
        return this.delegate.isEmpty();
    }

    public Set<String> getKeys() {
        return this.delegate.keySet();
    }

    public Collection<AbstractTag<?, ?>> getValues() {
        return this.delegate.values();
    }

    public AbstractTag<?, ?> get(String key) {
        return this.delegate.get(key);
    }

    public boolean hasKey(String key) {
        return this.delegate.containsKey(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getPrimitive(String key) {
        return Optional.ofNullable((PrimitiveTag<T>) get(key))
                .map(PrimitiveTag::getValue)
                .orElse(null);
    }

    public boolean hasKeyOfType(String key, byte typeId) {
        return hasKey(key) && get(key).getType().getId() == typeId;
    }

    public boolean hasKeyOfType(String key, TagType type) {
        return hasKey(key) && get(key).getType().equals(type);
    }

    public void put(String key, AbstractTag<?, ?> tag) {
        this.delegate.put(key, tag);
    }

    public void putPrimitive(String key, TagType type, Object val) {
        this.delegate.put(key, new PrimitiveTag<>(type, val));
    }

    public <IN> void putArray(String key, TagType of, Iterable<IN> array) {
        ArrayTag<?, ?, ?> tag;
        if (of.equals(ArrayTag.TYPE.get()) || of.equals(CompoundTag.TYPE.get())) {
            tag = new ArrayTag<>(of, array);
        } else {
            tag = new PrimitiveArrayTag<>(of, array);
        }

        this.delegate.put(key, tag);
    }

    public void putCompound(String key, Map<String, Object> map) {
        this.delegate.put(key, new CompoundTag(map));
    }

    public void putIfAbsent(String key, AbstractTag<?, ?> base) {
        if (!hasKey(key)) {
            put(key, base);
        }
    }

    @SuppressWarnings("unchecked")
    public <ARRAY extends ArrayTag<?, ?, ARRAY>> ARRAY getArray(String key) {
        return (ARRAY) get(key);
    }

    public <ARRAY extends ArrayTag<?, ?, ARRAY>> ARRAY getArrayOf(String key, TagType type) {
        ARRAY array = getArray(key);
        if (array != null && array.getTypeOf().equals(type)) {
            return array;
        } else {
            return null;
        }
    }

    public <T> PrimitiveArrayTag<T> getPrimitiveArray(String key) {
        return getArray(key);
    }

    public <T> PrimitiveArrayTag<T> getPrimitiveArrayOf(String key, TagType type) {
        return getArrayOf(key, type);
    }

    public <ARRAY extends ArrayTag<CompoundTag, CompoundTag, ARRAY>> ARRAY getArrayOfCompound(String key) {
        return getArray(key);
    }

    public <ARRAY extends ArrayTag<IN, IN, ARRAY>, IN extends ArrayTag<?, ?, IN>> ARRAY getArrayOfArray(String key) {
        return getArray(key);
    }

    public CompoundTag getCompound(String key) {
        return (CompoundTag) get(key);
    }

    public byte getByte(String key) {
        return getPrimitive(key);
    }

    public short getShort(String key) {
        return getPrimitive(key);
    }

    public int getInt(String key) {
        return getPrimitive(key);
    }

    public long getLong(String key) {
        return getPrimitive(key);
    }

    public float getFloat(String key) {
        return getPrimitive(key);
    }

    public double getDouble(String key) {
        return getPrimitive(key);
    }

    public byte[] getByteArray(String key) {
        return getPrimitive(key);
    }

    public String getString(String key) {
        return getPrimitive(key);
    }

    public int[] getIntArray(String key) {
        return getPrimitive(key);
    }

    public long[] getLongArray(String key) {
        return getPrimitive(key);
    }
}
