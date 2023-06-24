package me.alek.honey.netty.structure.converters;

import java.util.*;

public class SimpleMapConverter<K, V, T> implements MapConverter<K, V, T> {

    private final JavaConverter<V, T> converter;
    private final Map<K, T> delegatedMap = new HashMap<>();
    private Map<K, V> declaringMap;

    public SimpleMapConverter(JavaConverter<V, T> converter) {
        this.converter = converter;
    }

    public SimpleMapConverter<K, V, T> apply(Map<K, V> map) {
        declaringMap = map;
        for (Map.Entry<K, V> entry : map.entrySet()) {
            delegatedMap.put(entry.getKey(), converter.convertDelegate(entry.getValue()));
        }
        return this;
    }

    public void checkMap() {
        if (declaringMap == null) {
            throw new RuntimeException("Ingen delegated map for wrapper map");
        }
    }

    public Set<K> keySet() {
        checkMap();

        return delegatedMap.keySet();
    }

    public void clear() {
        checkMap();

        declaringMap.clear();
        delegatedMap.clear();
    }

    public boolean containsKey(K key) {
        checkMap();

        return delegatedMap.containsKey(key);
    }

    public T removeConverted(K key) {
        checkMap();

        delegatedMap.remove(key);
        return converter.convertDelegate(declaringMap.remove(key));
    }

    public T putConverted(K key, T value) {
        checkMap();

        delegatedMap.put(key, value);
        declaringMap.put(key, converter.convertDeclaring(value));
        return value;
    }

    public void putAllConverted(Map<? extends K, ? extends V> otherMap) {
        checkMap();

        for (Map.Entry<? extends K, ? extends V> entry : otherMap.entrySet()) {
            delegatedMap.put(entry.getKey(), converter.convertDelegate(entry.getValue()));
        }
        declaringMap.putAll(otherMap);
    }

    public void putAllOther(Map<? extends K, ? extends T> otherMap) {
        checkMap();

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
    public SimpleMapConverter<K, V, T> convertDelegate(Map<K, V> delegate) {
        checkMap();

        return this;
    }

    @Override
    public Map<K, V> convertDeclaring(MapConverter<K, V, T> object) {
        checkMap();

        return declaringMap;
    }
}
