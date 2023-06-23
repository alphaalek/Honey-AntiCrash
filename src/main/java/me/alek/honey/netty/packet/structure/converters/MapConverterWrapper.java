package me.alek.honey.netty.packet.structure.converters;

import java.util.*;

public class MapConverterWrapper<K, V, T> extends HashMap<K, V> implements JavaConverter<Map<K, V>, MapConverterWrapper<K, V, T>> {

    private final JavaConverter<V, T> converter;
    private final Map<K, T> delegatedMap = new HashMap<>();

    public MapConverterWrapper(JavaConverter<V, T> converter) {
        this.converter = converter;
    }

    public JavaConverter<Map<K, V>, MapConverterWrapper<K, V, T>> apply(Map<K, V> map) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            delegatedMap.put(entry.getKey(), converter.convertDelegate(entry.getValue()));
        }
        return this;
    }

    public T removeConverted(K key) {
        delegatedMap.remove(key);
        return converter.convertDelegate(super.remove(key));
    }

    public T putConverted(K key, T value) {
        delegatedMap.put(key, value);
        super.put(key, converter.convertDeclaring(value));
        return value;
    }

    public void putAll(Map<? extends K, ? extends V> otherMap) {
        for (Map.Entry<? extends K, ? extends V> entry : otherMap.entrySet()) {
            delegatedMap.put(entry.getKey(), converter.convertDelegate(entry.getValue()));
        }
        super.putAll(otherMap);
    }

    public void putAllConverted(Map<? extends K, ? extends T> otherMap) {
        for (Map.Entry<? extends K, ? extends T> entry : otherMap.entrySet()) {
            putConverted(entry.getKey(), entry.getValue());
        }
    }

    public Collection<T> convertedValues() {
        List<T> list = new ArrayList<>();
        for (V value : values()) {
            list.add(converter.convertDelegate(value));
        }
        return list;
    }

    public T getConverted(K key) {
        return converter.convertDelegate(super.get(key));
    }

    @Override
    public MapConverterWrapper<K, V, T> convertDelegate(Map<K, V> delegate) {
        return (MapConverterWrapper<K, V, T>) delegatedMap;
    }

    @Override
    public Map<K, V> convertDeclaring(MapConverterWrapper<K, V, T> object) {
        return this;
    }
}
