package me.alek.honey.netty.modelwrappers.nbt;

import me.alek.honey.netty.structure.converters.ApplicableConverter;
import me.alek.honey.netty.structure.converters.ListConverter;

import java.util.List;

public abstract class NBTAbstractListWrapper<V, T> extends NBTBase<List<V>, ListConverter<V, T>> implements ListConverter<V, T> {

    private final ListConverter<V, T> delegate;

    public NBTAbstractListWrapper(Object base, ListConverter<V, T> handle) {
        super(base, handle);
        this.delegate = handle;
    }

    @Override
    public ApplicableConverter<List<V>, ListConverter<V,T>> apply(List<V> list) {
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
    public T removeConverted(int index) {
        return delegate.removeConverted(index);
    }

    @Override
    public boolean addConverted(T value) {
        return delegate.addConverted(value);
    }

    @Override
    public T setConverted(int index, T value) {
        return delegate.setConverted(index, value);
    }

    @Override
    public boolean removeConverted(T value) {
        return delegate.removeConverted(value);
    }

    @Override
    public T getConverted(int index) {
        return delegate.getConverted(index);
    }

    @Override
    public boolean containsConverted(T value) {
        return delegate.containsConverted(value);
    }

    @Override
    public List<T> getDelegatedList() {
        return delegate.getDelegatedList();
    }

    @Override
    public ListConverter<V, T> convertDelegate(List<V> delegate) {
        return this.delegate.convertDelegate(delegate);
    }

    @Override
    public List<V> convertDeclaring(ListConverter<V, T> object) {
        return delegate.convertDeclaring(object);
    }
}
