package me.alek.honey.netty.modelwrappers.nbt;

import me.alek.honey.netty.packet.structure.ReflectStructure;
import me.alek.honey.netty.packet.structure.converters.Converters;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class NBTCompound extends NBTBase<Map<String, Object>, Map<String, NBTBase<?, ?>>> {

    public NBTCompound(Object rawNms) {
        super(rawNms);
    }

    @Override
    public Function<ReflectStructure<Object, ?>, ReflectStructure<Map<String, Object>, Map<String, NBTBase<?, ?>>>> getValueStructure() {
        return (structure) -> structure.withType(getParameterizedClass(), Converters.getNBTCompoundConverter());
    }

    @SuppressWarnings("unchecked")
    private <K, V> Class<Map<K, V>> getParameterizedClass() {
        return (Class<Map<K, V>>) (Class<?>) Map.class;
    }

    public NBTBase<?, ?> remove(String key) {
        return getValue().remove(key);
    }

    public void clear() {
        getValue().clear();
    }

    public Set<String> getKeys() {
        return getValue().keySet();
    }

    public Collection<NBTBase<?, ?>> getBases() {
        return getValue().values();
    }

    public NBTBase<?, ?> get(String key) {
        return getValue().get(key);
    }

    public boolean hasKey(String key) {
        return getValue().containsKey(key);
    }

    public boolean hasKeyOfType(String key, byte typeId) {
        return getValue().get(key).getType().getTypeId() == typeId;
    }

    public NBTList<?> getList(String key) {
        return null;
    }

    public <R> NBTList<R> getListOf(String key, NBTType nbtType) {
        NBTList<?> list = getList(key);
        if (list == null) {
            return null;
        }
        if (nbtType != list.getTypeOf()) {
            return null;
        }
        return (NBTList<R>) list;
    }

}
