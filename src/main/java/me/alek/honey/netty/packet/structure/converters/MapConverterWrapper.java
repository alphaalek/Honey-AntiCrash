package me.alek.honey.netty.packet.structure.converters;

import org.bukkit.Bukkit;

import java.util.*;

public class MapConverterWrapper<K, V, T> implements JavaConverter<Map<K, V>, MapConverterWrapper<K, V, T>> {

    private final JavaConverter<V, T> converter;
    private final Map<K, T> delegatedMap = new HashMap<>();
    private final Map<K, V> declaringMap = new HashMap<>();

    public MapConverterWrapper(JavaConverter<V, T> converter) {
        this.converter = converter;
    }

    public MapConverterWrapper<K, V, T> apply(Map<K, V> map) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            delegatedMap.put(entry.getKey(), converter.convertDelegate(entry.getValue()));
            declaringMap.put(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public Set<K> keySet() {
        return delegatedMap.keySet();
    }

    public void clear() {
        declaringMap.clear();
        delegatedMap.clear();
    }

    public boolean containsKey(K key) {
        return delegatedMap.containsKey(key);
    }

    public T removeConverted(K key) {
        delegatedMap.remove(key);
        return converter.convertDelegate(declaringMap.remove(key));
    }

    public T putConverted(K key, T value) {
        delegatedMap.put(key, value);
        declaringMap.put(key, converter.convertDeclaring(value));
        return value;
    }

    public void putAllConverted(Map<? extends K, ? extends V> otherMap) {
        for (Map.Entry<? extends K, ? extends V> entry : otherMap.entrySet()) {
            delegatedMap.put(entry.getKey(), converter.convertDelegate(entry.getValue()));
        }
        declaringMap.putAll(otherMap);
    }

    public void putAllOther(Map<? extends K, ? extends T> otherMap) {
        for (Map.Entry<? extends K, ? extends T> entry : otherMap.entrySet()) {
            declaringMap.put(entry.getKey(), converter.convertDeclaring(entry.getValue()));
        }
        delegatedMap.putAll(otherMap);
    }

    public Collection<T> convertedValues() {
        return delegatedMap.values();
    }

    public T getConverted(K key) {
        return delegatedMap.get(key);
    }

    @Override
    public MapConverterWrapper<K, V, T> convertDelegate(Map<K, V> delegate) {
        return this;
    }

    @Override
    public Map<K, V> convertDeclaring(MapConverterWrapper<K, V, T> object) {
        return declaringMap;
    }
}
