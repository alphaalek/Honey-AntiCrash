package me.alek.honey.netty.modelwrappers.nbt;

import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.structure.converters.MapConverter;

import java.io.DataOutput;
import java.util.Collection;
import java.util.Set;

public class NBTCompound extends NBTAbstractMapWrapper<String, Object, NBTBase<?, ?>> {

    public NBTCompound(Object rawNms, MapConverter<String, Object, NBTBase<?, ?>> converter) {
        super(rawNms, converter);
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
        return get(key).getType().getTypeId() == typeId;
    }

    public boolean hasKeyOfType(String key, NBTType nbtType) {
        return get(key).getType() == nbtType;
    }

    public void put(String key, NBTBase<?, ?> base) {
        getValue().putConverted(key, base);
    }

    public void putIfAbsent(String key, NBTBase<?, ?> base) {
        if (!hasKey(key)) {
            put(key, base);
        }
    }

    public <R> NBTList<R> getListOf(String key, NBTType nbtType) {
        return castNBTList(getAbstractListOf(key, nbtType));
    }

    @SuppressWarnings("unchecked")
    private <R> NBTList<R> castNBTList(NBTAbstractList<?> list) {
        return (NBTList<R>) list;
    }

    @SuppressWarnings("unchecked")
    public <R> NBTAbstractList<R> getAbstractListOf(String key, NBTType nbtType) {
        NBTList<?> list = getList(key);
        if (list == null) {
            return null;
        }
        if (nbtType != list.getTypeOf()) {
            return null;
        }
        try {
            return (NBTAbstractList<R>) list;
        } catch (Exception ex) {
            if (nbtType == NBTType.COMPOUND) {
                return (NBTAbstractList<R>) new NBTCompoundList(list.getHandle(), Converters.getNBTListConverter());
            }
            else if (nbtType == NBTType.LIST) {
                return (NBTAbstractList<R>) new NBTListInList(list.getHandle(), Converters.getNBTListConverter());
            }
            else {
                return (NBTAbstractList<R>) new NBTList<>(list.getHandle(), nbtType, Converters.getNBTListConverter());
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <R> NBTAbstractList<R> getAbstractListOf(String key, Class<R> clazz) {
        return getAbstractListOf(key, NBTType.getFromClass(clazz));
    }

    public <R> NBTList<R> getListOf(String key, Class<R> clazz) {
        return castNBTList(getAbstractListOf(key, clazz));
    }

    public NBTCompoundList getCompoundList(String key) {
        return (NBTCompoundList) getCompoundListParameterized(key);
    }

    private NBTAbstractList<NBTCompound> getCompoundListParameterized(String key) {
        return getAbstractListOf(key, NBTType.COMPOUND);
    }

    public NBTListInList getListInList(String key) {
        return (NBTListInList) getListInListParameterized(key);
    }

    private NBTAbstractList<NBTList<?>> getListInListParameterized(String key) {
        return getAbstractListOf(key, NBTType.LIST);
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

    public NBTList<?> getList(String key) {
        return (NBTList<?>) get(key);
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
