package me.alek.honey.netty.packet.structure.converters;

import java.util.ArrayList;
import java.util.List;

public class ListConverterWrapper<V, T> implements JavaConverter<List<V>, ListConverterWrapper<V, T>> {

    private final JavaConverter<V, T> converter;
    private final List<T> delegatedList = new ArrayList<>();
    private List<V> declaringList;

    public ListConverterWrapper(JavaConverter<V, T> converter) {
        this.converter = converter;
    }

    public ListConverterWrapper<V, T> apply(List<V> list) {
        declaringList = list;
        for (V value : list) {
            delegatedList.add(converter.convertDelegate(value));
        }
        return this;
    }

    public void checkList() {
        if (declaringList == null) {
            throw new RuntimeException("Ingen delegated list for wrapper list");
        }
    }

    public int size() {
        checkList();

        return delegatedList.size();
    }

    public void clear() {
        checkList();

        delegatedList.clear();
        declaringList.clear();
    }

    public boolean isEmpty() {
        checkList();

        return delegatedList.isEmpty();
    }

    public T removeConverted(int index) {
        checkList();

        delegatedList.remove(index);
        return converter.convertDelegate(declaringList.remove(index));
    }

    public boolean addConverted(T value) {
        checkList();

        delegatedList.add(value);
        return declaringList.add(converter.convertDeclaring(value));
    }

    public T setConverted(int index, T value) {
        checkList();

        delegatedList.set(index, value);
        return converter.convertDelegate(declaringList.set(index, converter.convertDeclaring(value)));
    }

    public boolean removeConverted(T value) {
        checkList();

        delegatedList.remove(value);
        return declaringList.remove(converter.convertDeclaring(value));
    }

    public T getConverted(int index) {
        checkList();

        return converter.convertDelegate(declaringList.get(index));
    }

    public boolean containsConverted(T value) {
        checkList();

        return delegatedList.contains(value);
    }

    public List<T> getDelegatedList() {
        checkList();

        return delegatedList;
    }

    @Override
    public ListConverterWrapper<V, T> convertDelegate(List<V> delegate) {
        checkList();

        return this;
    }

    @Override
    public List<V> convertDeclaring(ListConverterWrapper<V, T> object) {
        checkList();

        return declaringList;
    }
}
