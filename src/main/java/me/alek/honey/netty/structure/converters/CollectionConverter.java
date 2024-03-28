package me.alek.honey.netty.structure.converters;

import java.util.function.Predicate;

public interface CollectionConverter<K, V> extends Iterable<V> {

    int size();

    void clear();

    boolean isEmpty();

    boolean contains(K o);

    boolean containsConverted(V value);

    boolean remove(K o);

    boolean removeConverted(V value);

    boolean removeIfConverted(Predicate<V> value);

    boolean add(K value);

    boolean addConverted(V value);
}
