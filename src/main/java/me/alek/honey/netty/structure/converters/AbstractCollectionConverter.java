package me.alek.honey.netty.structure.converters;

import java.util.Collection;

public class AbstractCollectionConverter<K, V> implements CollectionConverter<K, V> {

    private Collection<K> declaringCollection;
    private Collection<V> delegatedCollection;
    private final JavaConverter<K, V> converter;

    public AbstractCollectionConverter(JavaConverter<K, V> converter) {
        this.converter = converter;
    }

    public void applyCollections(Collection<K> collection1, Collection<V> collection2) {
        this.declaringCollection = collection1;
        this.delegatedCollection = collection2;
    }

    protected void checkCollections() {
        if (declaringCollection == null) {
            throw new RuntimeException("Ingen delegated list for wrapper list");
        }
        int delegatedSize = delegatedCollection.size();
        int declaringSize = declaringCollection.size();

        if (delegatedSize != declaringSize) {

            delegatedCollection.clear();
            for (K declaring : declaringCollection) {
                delegatedCollection.add(converter.convertDelegate(declaring));
            }
        }
    }

    @Override
    public int size() {
        checkCollections();

        return declaringCollection.size();
    }

    @Override
    public void clear() {
        checkCollections();

        delegatedCollection.clear();
        declaringCollection.clear();
    }

    @Override
    public boolean isEmpty() {
        checkCollections();

        return declaringCollection.isEmpty();
    }

    @Override
    public boolean remove(K o) {
        checkCollections();

        declaringCollection.remove(o);
        return delegatedCollection.remove(converter.convertDelegate(o));
    }

    @Override
    public boolean removeConverted(V value) {
        return false;
    }

    @Override
    public boolean contains(K o) {
        checkCollections();

        return declaringCollection.contains(o);
    }

    @Override
    public boolean containsConverted(V value) {
        return contains(converter.convertDeclaring(value));
    }

    @Override
    public boolean add(K value) {
        checkCollections();

        declaringCollection.add(value);
        return delegatedCollection.add(converter.convertDelegate(value));
    }

    @Override
    public boolean addConverted(V value) {
        return add(converter.convertDeclaring(value));
    }

}
