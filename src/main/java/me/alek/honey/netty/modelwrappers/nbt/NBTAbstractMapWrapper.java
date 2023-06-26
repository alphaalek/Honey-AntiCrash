package me.alek.honey.netty.modelwrappers.nbt;

import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.converters.ApplicableConverter;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.structure.converters.JavaConverter;
import me.alek.honey.netty.structure.converters.MapConverter;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public abstract class NBTAbstractMapWrapper<K, V, T> extends NBTBase<Map<K, V>, MapConverter<K, V, T>> implements MapConverter<K, V, T> {

    private MapConverter<K, V, T> delegate;

    public NBTAbstractMapWrapper(Object base, NBTRoot root, NBTBase<?, ?> owner) {
        super(base, root, owner);
    }

    @Override
    public Function<ReflectStructure<Object, ?>, ReflectStructure<Map<K, V>, MapConverter<K, V, T>>> getValueStructure(ApplicableConverter<Map<K, V>, MapConverter<K, V, T>> converter, Map<K, V> original) {
        return (structure) -> structure.withSpecificType(Map.class, converter.apply(original), false);
    }

    public ApplicableConverter<Map<K, V>, MapConverter<K, V, T>> getConverter() {
        if (delegate == null) {
            delegate = (MapConverter<K, V, T>) Converters.getNBTCompoundConverter(this);
        }
        return delegate;
    }

    @Override
    public JavaConverter<V, T> getHandle() {
        return delegate.getHandle();
    }

    @Override
    public ApplicableConverter<Map<K,V>, MapConverter<K,V,T>> apply(Map<K, V> map) {
        return delegate.apply(map);
    }

    @Override
    public Set<K> keySet() {
        return delegate.keySet();
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public boolean containsKey(K key) {
        return delegate.containsKey(key);
    }

    @Override
    public T removeConverted(K key) {
        return delegate.removeConverted(key);
    }

    @Override
    public T putConverted(K key, T value) {
        return delegate.putConverted(key, value);
    }

    @Override
    public void putAllConverted(Map<? extends K, ? extends V> otherMap) {
        delegate.putAllConverted(otherMap);
    }

    @Override
    public void putAllOther(Map<? extends K, ? extends T> otherMap) {
        delegate.putAllOther(otherMap);
    }

    @Override
    public Collection<T> convertedValues() {
        return delegate.convertedValues();
    }

    @Override
    public T getConverted(K key) {
        return delegate.getConverted(key);
    }

    @Override
    public MapConverter<K, V, T> convertDelegate(Map<K, V> delegate) {
        return this.delegate.convertDelegate(delegate);
    }

    @Override
    public Map<K, V> convertDeclaring(MapConverter<K, V, T> object) {
        return delegate.convertDeclaring(object);
    }
}
