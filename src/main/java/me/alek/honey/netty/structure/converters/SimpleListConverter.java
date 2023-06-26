package me.alek.honey.netty.structure.converters;

import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

public class SimpleListConverter<V, T> implements ListConverter<V, T> {

    private final JavaConverter<V, T> converter;
    private final List<T> delegatedList = new ArrayList<>();
    private List<V> declaringList;

    public SimpleListConverter(JavaConverter<V, T> converter) {
        this.converter = converter;
    }

    public SimpleListConverter<V, T> apply(List<V> list) {
        declaringList = list;
        for (V value : list) {
            delegatedList.add(converter.convertDelegate(value));
        }
        return this;
    }

    @Override
    public ListConverter<V, T> fakeApply(List<V> mapping) {
        return new SimpleListConverter<>(converter).apply(mapping);
    }

    public void checkList() {
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

    @Override
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

        return delegatedList.get(index);
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
    public JavaConverter<V, T> getHandle() {
        return converter;
    }

    @Override
    public SimpleListConverter<V, T> convertDelegate(List<V> delegate) {
        checkList();

        return this;
    }

    @Override
    public List<V> convertDeclaring(ListConverter<V, T> object) {
        checkList();

        return declaringList;
    }
}
