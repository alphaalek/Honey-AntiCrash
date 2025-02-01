package me.alek.honey.netty.structure.converters;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface MapConverter<K, V, T> extends JavaConverter<Map<K, V>, MapConverter<K, V, T>>, ApplicableConverter<Map<K, V>, MapConverter<K, V, T>> {

    boolean containsKey(K key);

    boolean isEmpty();

    Set<K> keySet();

    void clear();

    int size();

    T removeConverted(K key);

    T putConverted(K key, T value);

    void putAllConverted(Map<? extends K, ? extends V> otherMap);

    void putAllOther(Map<? extends K, ? extends T> otherMap);

    Collection<T> convertedValues();

    Map<K, T> getDelegatedMap();

    T getConverted(K key);

    JavaConverter<V, T> getHandle();
}
