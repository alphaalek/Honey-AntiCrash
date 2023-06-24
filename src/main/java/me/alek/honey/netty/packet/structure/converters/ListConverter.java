package me.alek.honey.netty.packet.structure.converters;

import java.util.List;

public interface ListConverter<V, T> extends JavaConverter<List<V>, ListConverter<V, T>>, ApplicableConverter<List<V>, ListConverter<V, T>> {

    int size();
    void clear();
    boolean isEmpty();
    T removeConverted(int index);
    boolean addConverted(T value);
    T setConverted(int index, T value);
    boolean removeConverted(T value);
    T getConverted(int index);
    boolean containsConverted(T value);
    List<T> getDelegatedList();
}
