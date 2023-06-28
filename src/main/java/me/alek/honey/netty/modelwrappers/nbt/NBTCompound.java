package me.alek.honey.netty.modelwrappers.nbt;

import me.alek.honey.netty.modelwrappers.nbt.lists.NBTList;
import me.alek.honey.netty.modelwrappers.nbt.lists.NBTListOfCompound;
import me.alek.honey.netty.modelwrappers.nbt.lists.NBTListOfList;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.converters.ApplicableConverter;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.structure.converters.JavaConverter;
import me.alek.honey.netty.structure.converters.MapConverter;

import java.io.DataOutput;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class NBTCompound extends NBTBase<Map<String, Object>, MapConverter<String, Object, NBTBase<?, ?>>> {

    private MapConverter<String, Object, NBTBase<?, ?>> delegate;

    public NBTCompound(Object rawNms, NBTRoot root, NBTBase<?, ?> owner) {
        super(rawNms, root, owner);
    }

    @Override
    public Function<ReflectStructure<Object, ?>, ReflectStructure<Map<String, Object>, MapConverter<String, Object, NBTBase<?, ?>>>> applyStructure(ApplicableConverter<Map<String, Object>, MapConverter<String, Object, NBTBase<?, ?>>> converter) {
        return (structure) -> structure.withSpecificType(Map.class, converter, false);
    }

    public ApplicableConverter<Map<String, Object>, MapConverter<String, Object, NBTBase<?, ?>>> getConverter() {
        if (delegate == null) {
            delegate = Converters.getNBTCompoundConverter(this);
        }
        return delegate;
    }

    public JavaConverter<Object, NBTBase<?, ?>> getConverterHandle() {
        return delegate.getHandle();
    }

    public NBTBase<?, ?> remove(String key) {
        return getValue().removeConverted(key);
    }

    public void clear() {
        getValue().clear();
    }

    public Set<String> getKeys() {
        return getValue().keySet();
    }

    public Collection<NBTBase<?, ?>> getBases() {
        return getValue().convertedValues();
    }

    public NBTBase<?, ?> get(String key) {
        return getValue().getConverted(key);
    }

    public boolean hasKey(String key) {
        return getValue().containsKey(key);
    }

    public boolean hasKeyOfType(String key, byte typeId) {
        return hasKey(key) && get(key).getType().getTypeId() == typeId;
    }

    public boolean hasKeyOfType(String key, NBTType nbtType) {
        return hasKey(key) && get(key).getType() == nbtType;
    }

    public void put(String key, NBTBase<?, ?> base) {
        getValue().putConverted(key, base);
    }

    public void putIfAbsent(String key, NBTBase<?, ?> base) {
        if (!hasKey(key)) {
            put(key, base);
        }
    }

    @Override
    public void write(DataOutput output) throws Exception {
        for (String key : getValue().keySet()) {
            NBTBase<?, ?> base = getValue().getConverted(key);
            output.writeByte((byte) getType().getTypeId());
            if (getType() == NBTType.END) {
                continue;
            }
            output.writeUTF(key);
            base.write(output);
        }
        output.writeByte(0);
    }

    @SuppressWarnings("unchecked")
    private <R> NBTList<R> castNBTList(NBTAbstractList<?> list) {
        return (NBTList<R>) list;
    }

    @SuppressWarnings("unchecked")
    public <R extends NBTBase<?, ?>> NBTAbstractList<R> getAbstractListOf(String key, NBTType nbtType) {

        NBTAbstractList<R> list = getAbstractList(key);
        if (list != null && nbtType == list.getTypeOf()) {
            return list;
        }
        NBTRoot root = getRoot();
        NBTBase<?, ?> base = get(key);

        if (base == null) {
            return null;
        }
        if (nbtType == NBTType.COMPOUND) {
            return (NBTAbstractList<R>) new NBTListOfCompound(base.getHandle(), root, base.getOwner());
        }
        else if (nbtType == NBTType.LIST) {
            return (NBTAbstractList<R>) new NBTListOfList(base.getHandle(), root, base.getOwner());
        }
        else {
            return (NBTAbstractList<R>) new NBTList<>(base.getHandle(), nbtType, root, base.getOwner());
        }
    }

    @SuppressWarnings("unchecked")
    public <R extends NBTBase<?, ?>> NBTAbstractList<R> getAbstractListOf(String key, Class<?> clazz) {
        return getAbstractListOf(key, NBTType.getFromClass(clazz));
    }

    @SuppressWarnings("unchecked")
    public <R> NBTList<R> getListOf(String key, Class<R> clazz) {
        return (NBTList<R>) (NBTAbstractList<?>) getAbstractListOf(key, clazz);
    }

    @SuppressWarnings("unchecked")
    public <R> NBTList<R> getListOf(String key, NBTType nbtType) {
        return getListOf(key, (Class<R>) nbtType.getClazz());
    }

    public NBTListOfCompound getListOfCompound(String key) {
        return (NBTListOfCompound) (NBTAbstractList<?>) getAbstractListOf(key, NBTType.COMPOUND);
    }

    public NBTListOfList getListOfList(String key) {
        return (NBTListOfList) (NBTAbstractList<?>) getAbstractListOf(key, NBTType.LIST);
    }

    @SuppressWarnings("unchecked")
    public <T> T getSpecific(String key) {
        if (hasKey(key)) {
            try {
                return (T) get(key).getValue();
            } catch (ClassCastException ex) {
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <R extends NBTBase<?, ?>> NBTAbstractList<R> getAbstractList(String key) {
        return (NBTAbstractList<R>) get(key);
    }

    public NBTCompound getCompound(String key) {
        return (NBTCompound) get(key);
    }

    public byte getByte(String key) {
        return getSpecific(key);
    }

    public short getShort(String key) {
        return getSpecific(key);
    }

    public int getInt(String key) {
        return getSpecific(key);
    }

    public long getLong(String key) {
        return getSpecific(key);
    }

    public float getFloat(String key) {
        return getSpecific(key);
    }

    public double getDouble(String key) {
        return getSpecific(key);
    }

    public byte[] getByteArray(String key) {
        return getSpecific(key);
    }

    public String getString(String key) {
        return getSpecific(key);
    }

    public int[] getIntArray(String key) {
        return getSpecific(key);
    }

    public long[] getLongArray(String key) {
        return getSpecific(key);
    }
}
