package me.alek.honey.netty.packet.structure.converters;

import java.util.HashMap;
import java.util.Map;

public class MapConverter<K, V, T> extends HashMap<K, V> implements TwinConverter<Map<K, V>, Map<K, T>> {

    private final TwinConverter<V, T> converter;
    private final Map<K, T> delegatedMap = new HashMap<>();

    public MapConverter(TwinConverter<V, T> converter) {
        this.converter = converter;
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

    public T getConverted(K key) {
        return converter.convertDelegate(super.get(key));
    }

    @Override
    public Map<K, T> convertDelegate(Map<K, V> delegate) {
        return delegatedMap;
    }

    @Override
    public Map<K, V> convertDeclaring(Map<K, T> object) {
        return this;
    }
}
