package me.alek.honey.netty.packet.structure.converters;

import java.util.ArrayList;
import java.util.List;

public class ListConverter<V, T> extends ArrayList<V> implements TwinConverter<List<V>, List<T>> {

    private final TwinConverter<V, T> converter;
    private final List<T> delegatedList = new ArrayList<>();

    public ListConverter(TwinConverter<V, T> converter) {
        this.converter = converter;
    }

    public boolean addConverted(T value) {
        delegatedList.add(value);
        return super.add(converter.convertDeclaring(value));
    }

    public boolean removeConverted(T value) {
        delegatedList.remove(value);
        return super.remove(converter.convertDeclaring(value));
    }

    public T getConverted(int index) {
        return converter.convertDelegate(super.get(index));
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
