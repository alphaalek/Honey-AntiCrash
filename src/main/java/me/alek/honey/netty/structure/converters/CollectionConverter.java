package me.alek.honey.netty.structure.converters;

public interface CollectionConverter<K, V> {

    int size();

    void clear();

    boolean isEmpty();

    boolean contains(K o);

    boolean containsConverted(V value);

    boolean remove(K o);

    boolean removeConverted(V value);

    boolean add(K value);

    boolean addConverted(V value);
}
