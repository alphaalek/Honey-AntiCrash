package me.alek.honey.netty.packet.structure.converters;

import java.util.ArrayList;
import java.util.List;

public class ListConverterWrapper<V, T> implements JavaConverter<List<V>, ListConverterWrapper<V, T>> {

    private final JavaConverter<V, T> converter;
    private final List<T> delegatedList = new ArrayList<>();
    private final List<V> declaringList = new ArrayList<>();

    public ListConverterWrapper(JavaConverter<V, T> converter) {
        this.converter = converter;
    }

    public ListConverterWrapper<V, T> apply(List<V> list) {
        declaringList.addAll(list);
        for (V value : list) {
            delegatedList.add(converter.convertDelegate(value));
        }
        return this;
    }

    public int size() {
        return delegatedList.size();
    }

    public void clear() {
        delegatedList.clear();
        declaringList.clear();
    }

    public boolean isEmpty() {
        return delegatedList.isEmpty();
    }

    public T removeConverted(int index) {
        delegatedList.remove(index);
        return converter.convertDelegate(declaringList.remove(index));
    }

    public boolean addConverted(T value) {
        delegatedList.add(value);
        return declaringList.add(converter.convertDeclaring(value));
    }

    public T setConverted(int index, T value) {
        delegatedList.set(index, value);
        return converter.convertDelegate(declaringList.set(index, converter.convertDeclaring(value)));
    }

    public boolean removeConverted(T value) {
        delegatedList.remove(value);
        return declaringList.remove(converter.convertDeclaring(value));
    }

    public T getConverted(int index) {
        return converter.convertDelegate(declaringList.get(index));
    }

    public boolean containsConverted(T value) {
        return delegatedList.contains(value);
    }

    public List<T> getDelegatedList() {
        return delegatedList;
    }

    @Override
    public ListConverterWrapper<V, T> convertDelegate(List<V> delegate) {
        return this;
    }

    @Override
    public List<V> convertDeclaring(ListConverterWrapper<V, T> object) {
        return declaringList;
    }
}
