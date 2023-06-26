package me.alek.honey.netty.modelwrappers.nbt;

import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.converters.*;

import java.util.List;
import java.util.function.Function;

public abstract class NBTAbstractListWrapper<T, V> extends NBTBase<List<T>, ListConverter<T, V>> implements ListConverter<T, V> {

    private ListConverter<T, V> delegate;

    public NBTAbstractListWrapper(Object base, NBTRoot root, NBTBase<?, ?> owner) {
        super(base, root, owner);
    }

    @Override
    public ApplicableConverter<List<T>, ListConverter<T, V>> getConverter() {
        if (delegate == null) {
            delegate = (ListConverter<T, V>) Converters.getNBTListConverter(getOwner());
        }
        return delegate;
    }

    @Override
    public Function<ReflectStructure<Object, ?>, ReflectStructure<List<T>, ListConverter<T, V>>> getValueStructure(ApplicableConverter<List<T>, ListConverter<T, V>> converter, List<T> original) {
        return (structure) -> structure.withSpecificType(List.class, converter.apply(original), false);
    }

    @Override
    public JavaConverter<T, V> getHandle() {
        return delegate.getHandle();
    }

    @Override
    public ApplicableConverter<List<T>, ListConverter<T, V>> apply(List<T> list) {
        return delegate.apply(list);
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public V removeConverted(int index) {
        return delegate.removeConverted(index);
    }

    @Override
    public boolean addConverted(V value) {
        return delegate.addConverted(value);
    }

    @Override
    public V setConverted(int index, V value) {
        return delegate.setConverted(index, value);
    }

    @Override
    public boolean removeConverted(V value) {
        return delegate.removeConverted(value);
    }

    @Override
    public V getConverted(int index) {
        return delegate.getConverted(index);
    }

    @Override
    public boolean containsConverted(V value) {
        return delegate.containsConverted(value);
    }

    @Override
    public List<V> getDelegatedList() {
        return delegate.getDelegatedList();
    }

    @Override
    public ListConverter<T, V> convertDelegate(List<T> delegate) {
        return this.delegate.convertDelegate(delegate);
    }

    @Override
    public List<T> convertDeclaring(ListConverter<T, V> object) {
        return delegate.convertDeclaring(object);
    }
}
