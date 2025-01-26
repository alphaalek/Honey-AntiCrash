package me.alek.honey.netty.structure.converters.wrapper;

import me.alek.honey.netty.structure.converters.JavaConverter;
import me.alek.honey.netty.structure.converters.MapConverter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SimpleMapConverter<K, V, T> implements MapConverter<K, V, T> {

    private final JavaConverter<V, T> converter;
    private final Map<K, T> delegatedMap = new HashMap<>();
    private Map<K, V> declaringMap = new HashMap<>();

    public SimpleMapConverter(JavaConverter<V, T> converter) {
        this.converter = converter;
    }

    public boolean isInvalid() {
        return declaringMap == null;
    }

    public SimpleMapConverter<K, V, T> inject(Map<K, V> map) {
        this.declaringMap = map;

        for (Map.Entry<K, V> entry : map.entrySet()) {
            this.delegatedMap.put(entry.getKey(), this.converter.convertDelegate(entry.getValue()));
        }

        return this;
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

    @Override
    public int size() {
        return delegatedMap.size();
    }

    public boolean containsKey(K key) {
        checkMap();

        return delegatedMap.containsKey(key);
    }

    @Override
    public boolean isEmpty() {
        checkMap();

        return delegatedMap.isEmpty();
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
