package me.alek.honey.netty.packet.structure.converters;

import java.util.ArrayList;
import java.util.List;

public class ListConverterWrapper<V, T> extends ArrayList<V> implements JavaConverter<List<V>, List<T>> {

    private final JavaConverter<V, T> converter;
    private final List<T> delegatedList = new ArrayList<>();

    public ListConverterWrapper(JavaConverter<V, T> converter) {
        this.converter = converter;
    }

    public JavaConverter<List<V>, List<T>> apply(List<V> list) {
        for (V value : list) {
            delegatedList.add(converter.convertDelegate(value));
        }
        return this;
    }

    public T removeConverted(int index) {
        delegatedList.remove(index);
        return converter.convertDelegate(super.remove(index));
    }

    public boolean addConverted(T value) {
        delegatedList.add(value);
        return super.add(converter.convertDeclaring(value));
    }

    public T setConverted(int index, T value) {
        delegatedList.set(index, value);
        return converter.convertDelegate(super.set(index, converter.convertDeclaring(value)));
    }

    public boolean removeConverted(T value) {
        delegatedList.remove(value);
        return super.remove(converter.convertDeclaring(value));
    }

    public T getConverted(int index) {
        return converter.convertDelegate(super.get(index));
    }

    public boolean containsConverted(T value) {
        return delegatedList.contains(value);
    }

    public List<T> getDelegatedList() {
        return delegatedList;
    }

    @Override
    public List<T> convertDelegate(List<V> delegate) {
        return delegatedList;
    }

    @Override
    public List<V> convertDeclaring(List<T> object) {
        return this;
    }
}
