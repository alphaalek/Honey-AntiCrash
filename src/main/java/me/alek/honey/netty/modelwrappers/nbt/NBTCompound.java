package me.alek.honey.netty.modelwrappers.nbt;

import me.alek.honey.netty.packet.structure.ReflectStructure;
import me.alek.honey.netty.packet.structure.converters.Converters;
import me.alek.honey.netty.packet.structure.converters.MapConverterWrapper;
import org.bukkit.Bukkit;

import java.io.DataOutput;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class NBTCompound extends NBTBase<Map<String, Object>, MapConverterWrapper<String, Object, NBTBase<?, ?>>> {

    public NBTCompound(Object rawNms) {
        super(rawNms);
    }

    @Override
    public Function<ReflectStructure<Object, ?>, ReflectStructure<Map<String, Object>, MapConverterWrapper<String, Object, NBTBase<?, ?>>>> getValueStructure(Map<String, Object> original) {
        return (structure) -> structure.withSpecificType(Map.class, Converters.getNBTCompoundConverter().apply(original), false);
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

    public void put(String key, NBTBase<?, ?> base) {
        getValue().putConverted(key, base);
    }

    public void putIfAbsent(String key, NBTBase<?, ?> base) {
        if (!hasKey(key)) {
            put(key, base);
        }
    }

    @SuppressWarnings("unchecked")
    public <R extends NBTBase<?, R>> NBTList<R> getListOf(String key, NBTType nbtType) {
        NBTList<?> list = getList(key);
        if (list == null) {
            return null;
        }
        if (nbtType != list.getTypeOf()) {
            return null;
        }
        try {
            return (NBTList<R>) list;
        } catch (Exception ex) {
            return new NBTList<>(list.getHandle(), nbtType);
        }
    }

    @SuppressWarnings("unchecked")
    public <R> NBTList<R> getListOf(String key, Class<R> clazz) {
        return (NBTList<R>) getListOf(key, NBTType.getFromClass(clazz));
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
