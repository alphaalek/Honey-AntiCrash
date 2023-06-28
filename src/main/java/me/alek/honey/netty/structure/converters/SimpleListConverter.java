package me.alek.honey.netty.structure.converters;

import java.util.ArrayList;
import java.util.List;

public class SimpleListConverter<V, T> implements ListConverter<V, T> {

    private final boolean controlDeclaring;
    private final JavaConverter<V, T> converter;
    private final List<T> delegatedList = new ArrayList<>();
    private List<V> declaringList;

    public SimpleListConverter(JavaConverter<V, T> converter, boolean controlDeclaring) {
        this.converter = converter;
        this.controlDeclaring = controlDeclaring;
    }

    public SimpleListConverter<V, T> apply(List<V> list) {
        if (controlDeclaring) {
            declaringList = list;
        }
        else {
            declaringList = new ArrayList<>();
        }
        for (V value : list) {
            declaringList.add(value);
            delegatedList.add(converter.convertDelegate(value));
        }
        return this;
    }

    @Override
    public ListConverter<V, T> fakeApply(List<V> mapping) {
        return new SimpleListConverter<>(converter, false).apply(mapping);
    }

    public void checkLists() {
        if (declaringList == null) {
            throw new RuntimeException("Ingen delegated list for wrapper list");
        }
        int delegatedSize = delegatedList.size();
        int declaringSize = declaringList.size();

        if (delegatedSize != declaringSize) {

            delegatedList.clear();
            for (V declaring : declaringList) {
                delegatedList.add(converter.convertDelegate(declaring));
            }
        }
    }

    public int size() {
        checkLists();

        return delegatedList.size();
    }

    public void clear() {
        checkLists();

        delegatedList.clear();
        declaringList.clear();
    }

    public boolean isEmpty() {
        checkLists();

        return delegatedList.isEmpty();
    }

    public T removeConverted(int index) {
        checkLists();

        delegatedList.remove(index);
        return converter.convertDelegate(declaringList.remove(index));
    }

    @Override
    public boolean addConverted(T value) {
        checkLists();

        delegatedList.add(value);
        return declaringList.add(converter.convertDeclaring(value));
    }

    public T setConverted(int index, T value) {
        checkLists();

        delegatedList.set(index, value);
        return converter.convertDelegate(declaringList.set(index, converter.convertDeclaring(value)));
    }

    public boolean removeConverted(T value) {
        checkLists();

        delegatedList.remove(value);
        return declaringList.remove(converter.convertDeclaring(value));
    }

    public T getConverted(int index) {
        checkLists();

        return delegatedList.get(index);
    }

    public boolean containsConverted(T value) {
        checkLists();

        return delegatedList.contains(value);
    }

    public List<T> getDelegatedList() {
        checkLists();

        return delegatedList;
    }

    @Override
    public JavaConverter<V, T> getHandle() {
        return converter;
    }

    @Override
    public SimpleListConverter<V, T> convertDelegate(List<V> delegate) {
        checkLists();

        return this;
    }

    @Override
    public List<V> convertDeclaring(ListConverter<V, T> object) {
        checkLists();

        return declaringList;
    }
}
