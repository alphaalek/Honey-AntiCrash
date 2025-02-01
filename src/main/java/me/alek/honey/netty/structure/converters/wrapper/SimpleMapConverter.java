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

        int delegatedSize = this.delegatedMap.size();
        int declaringSize = this.declaringMap.size();

        if (delegatedSize != declaringSize) {
            this.delegatedMap.clear();
            for (K declaringKey : this.declaringMap.keySet()) {
                this.delegatedMap.put(declaringKey, this.converter.convertDelegate(this.declaringMap.get(declaringKey)));
            }
        }

        return false;
    }

    public Set<K> keySet() {
        checkMap();

        return this.delegatedMap.keySet();
    }

    public void clear() {
        if (checkMap()) return;

        this.declaringMap.clear();
        this.delegatedMap.clear();
    }

    @Override
    public int size() {
        return this.delegatedMap.size();
    }

    public boolean containsKey(K key) {
        checkMap();

        return this.delegatedMap.containsKey(key);
    }

    @Override
    public boolean isEmpty() {
        checkMap();

        return this.delegatedMap.isEmpty();
    }

    public T removeConverted(K key) {
        if (checkMap()) return null;

        this.delegatedMap.remove(key);
        return this.converter.convertDelegate(this.declaringMap.remove(key));
    }

    public T putConverted(K key, T value) {
        if (checkMap()) return null;

        this.delegatedMap.put(key, value);
        this.declaringMap.put(key, this.converter.convertDeclaring(value));
        return value;
    }

    public void putAllConverted(Map<? extends K, ? extends V> otherMap) {
        if (checkMap()) return;

        for (Map.Entry<? extends K, ? extends V> entry : otherMap.entrySet()) {
            this.delegatedMap.put(entry.getKey(), this.converter.convertDelegate(entry.getValue()));
        }
        this.declaringMap.putAll(otherMap);
    }

    public void putAllOther(Map<? extends K, ? extends T> otherMap) {
        if (checkMap()) return;

        for (Map.Entry<? extends K, ? extends T> entry : otherMap.entrySet()) {
            this.declaringMap.put(entry.getKey(), this.converter.convertDeclaring(entry.getValue()));
        }
        this.delegatedMap.putAll(otherMap);
    }

    public Collection<T> convertedValues() {
        checkMap();

        return this.delegatedMap.values();
    }

    @Override
    public Map<K, T> getDelegatedMap() {
        checkMap();

        return this.delegatedMap;
    }

    public T getConverted(K key) {
        checkMap();

        return this.delegatedMap.get(key);
    }

    @Override
    public JavaConverter<V, T> getHandle() {
        return this.converter;
    }

    @Override
    public SimpleMapConverter<K, V, T> convertDelegate(Map<K, V> delegate, Object... params) {
        checkMap();

        return this;
    }

    @Override
    public Map<K, V> convertDeclaring(MapConverter<K, V, T> object) {
        checkMap();

        return this.declaringMap;
    }
}
