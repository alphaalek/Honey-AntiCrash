package me.alek.honey.netty.structure.converters.dedicated;

import me.alek.honey.netty.structure.converters.JavaConverter;
import me.alek.honey.netty.structure.converters.MapConverter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SimpleMapConverter<K, V, T> implements MapConverter<K, V, T> {

    private final boolean controlDeclaring;
    private final JavaConverter<V, T> converter;
    private final Map<K, T> delegatedMap = new HashMap<>();
    private Map<K, V> declaringMap;

    public SimpleMapConverter(JavaConverter<V, T> converter, boolean controlDeclaring) {
        this.converter = converter;
        this.controlDeclaring = controlDeclaring;
    }

    public boolean isInvalid() {
        return declaringMap == null;
    }

    public SimpleMapConverter<K, V, T> apply(Map<K, V> map) {
        if (controlDeclaring) {
            declaringMap = map;
        } else {
            declaringMap = new HashMap<>();
        }

        synchronized (declaringMap) {
            for (Map.Entry<K, V> entry : map.entrySet()) {
                if (!controlDeclaring) {
                    declaringMap.put(entry.getKey(), entry.getValue());
                }
                delegatedMap.put(entry.getKey(), converter.convertDelegate(entry.getValue()));
            }
        }

        return this;
    }

    @Override
    public MapConverter<K, V, T> fakeApply(Map<K, V> mapping) {
        return new SimpleMapConverter<K, V, T>(converter, false).apply(mapping);
    }

    public boolean checkMap() {
        if (declaringMap == null) {
            return true;
        }

        int delegatedSize = delegatedMap.size();
        int declaringSize = declaringMap.size();

        if (delegatedSize != declaringSize) {
            delegatedMap.clear();
            for (K declaringKey : declaringMap.keySet()) {
                delegatedMap.put(declaringKey, converter.convertDelegate(declaringMap.get(declaringKey)));
            }
        }

        return false;
    }

    public Set<K> keySet() {
        checkMap();

        return delegatedMap.keySet();
    }

    public void clear() {
        if (checkMap()) return;

        declaringMap.clear();
        delegatedMap.clear();
    }

    public boolean containsKey(K key) {
        checkMap();

        return delegatedMap.containsKey(key);
    }

    public T removeConverted(K key) {
        if (checkMap()) return null;

        delegatedMap.remove(key);
        return converter.convertDelegate(declaringMap.remove(key));
    }

    public T putConverted(K key, T value) {
        if (checkMap()) return null;

        delegatedMap.put(key, value);
        declaringMap.put(key, converter.convertDeclaring(value));
        return value;
    }

    public void putAllConverted(Map<? extends K, ? extends V> otherMap) {
        if (checkMap()) return;

        for (Map.Entry<? extends K, ? extends V> entry : otherMap.entrySet()) {
            delegatedMap.put(entry.getKey(), converter.convertDelegate(entry.getValue()));
        }
        declaringMap.putAll(otherMap);
    }

    public void putAllOther(Map<? extends K, ? extends T> otherMap) {
        if (checkMap()) return;

        for (Map.Entry<? extends K, ? extends T> entry : otherMap.entrySet()) {
            declaringMap.put(entry.getKey(), converter.convertDeclaring(entry.getValue()));
        }
        delegatedMap.putAll(otherMap);
    }

    public Collection<T> convertedValues() {
        checkMap();

        return delegatedMap.values();
    }

    public T getConverted(K key) {
        checkMap();

        return delegatedMap.get(key);
    }

    @Override
    public Object getRaw(K key) {
        return declaringMap.get(key);
    }

    @Override
    public JavaConverter<V, T> getHandle() {
        return converter;
    }

    @Override
    public SimpleMapConverter<K, V, T> convertDelegate(Map<K, V> delegate, Object... params) {
        checkMap();

        return this;
    }

    @Override
    public Map<K, V> convertDeclaring(MapConverter<K, V, T> object) {
        checkMap();

        return declaringMap;
    }
}
