package me.alek.honey.netty.modelwrappers;

import me.alek.honey.netty.utility.reflect.NMSUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class WrappedLongObjectHashMap<V> extends WrappedObject<WrappedLongObjectHashMap<V>> {

    private final Class<V> clazz;

    public WrappedLongObjectHashMap(Object handle) {
        this(handle, (Class<V>) NMSUtils.getChunkClass());
    }

    public WrappedLongObjectHashMap(Object handle, Class<V> clazz) {
        super(handle);
        this.clazz = clazz;
    }

    public int size() {
        return getModifier(int.class).invokeMethod("size", 0);
    }

    public boolean isEmpty() {
        return getModifier(boolean.class).invokeMethod("isEmpty", 0);
    }

    public boolean containsKey(long key) {
        return getModifier(boolean.class).invokeMethod("containsKey", 0, key);
    }

    public boolean containsValue(V value) {
        return getModifier(boolean.class).invokeMethod("containsValue", 0, value);
    }

    public V get(long key) {
        return getModifier(clazz).invokeMethod("get", 0, key);
    }

    public V put(long key, V value) {
        return getModifier(clazz).invokeMethod("put", 0, key, value);
    }

    public V remove(long key) {
        return getModifier(clazz).invokeMethod("remove", 0, key);
    }

    public void putAll(Map<? extends Long, ? extends V> map) {
        getModifier(void.class).invokeMethod("putAll", 0, map);
    }

    public void clear() {
        getModifier(void.class).invokeMethod("clear", 0);
    }

    public Set<Long> keySet() {
        return getModifier(Set.class).invokeMethod("keySet", 0);
    }

    public Collection<V> values() {
        return getModifier(Collection.class).invokeMethod("values", 0);
    }

    public Set<Map.Entry<Long, V>> entrySet() {
        return getModifier(Set.class).invokeMethod("entrySet", 0);
    }
}
