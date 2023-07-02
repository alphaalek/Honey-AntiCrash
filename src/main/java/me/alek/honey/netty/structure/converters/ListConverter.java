package me.alek.honey.netty.structure.converters;

import java.util.List;

public interface ListConverter<V, T> extends CollectionConverter<V, T>, JavaConverter<List<V>, ListConverter<V, T>>, ApplicableConverter<List<V>, ListConverter<V, T>> {

    T removeConverted(int index);

    T setConverted(int index, T value);

    T getConverted(int index);

    List<T> getDelegatedList();

    JavaConverter<V, T> getHandle();
}
