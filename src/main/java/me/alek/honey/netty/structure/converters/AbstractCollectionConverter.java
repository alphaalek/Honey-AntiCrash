package me.alek.honey.netty.structure.converters;

import org.bukkit.Bukkit;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Predicate;

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

    public boolean isInvalid() {
        return declaringCollection == null || delegatedCollection == null;
    }

    protected boolean checkCollections() {
        if (declaringCollection == null) {
            return true;
        }
        int delegatedSize = delegatedCollection.size();
        int declaringSize = declaringCollection.size();

        if (delegatedSize != declaringSize) {

            delegatedCollection.clear();
            for (K declaring : declaringCollection) {
                delegatedCollection.add(converter.convertDelegate(declaring));
            }
        }
        return delegatedSize != declaringSize;
    }

    @Override
    public int size() {
        if (checkCollections()) return 0;

        return declaringCollection.size();
    }

    @Override
    public void clear() {
        if (checkCollections()) return;

        delegatedCollection.clear();
        declaringCollection.clear();
    }

    @Override
    public boolean isEmpty() {
        if (checkCollections()) return true;

        return declaringCollection.isEmpty();
    }

    @Override
    public boolean remove(K o) {
        if (checkCollections()) return false;

        declaringCollection.remove(o);
        return delegatedCollection.remove(converter.convertDelegate(o));
    }

    @Override
    public boolean removeConverted(V value) {
        if (checkCollections()) return false;

        declaringCollection.remove(converter.convertDeclaring(value));
        return delegatedCollection.remove(value);
    }

    @Override
    public boolean removeIfConverted(Predicate<V> tester) {
        Iterator<V> iteratorConverted = iterator();
        Iterator<K> iterator = declaringCollection.iterator();
        boolean removed = false;
        while (iteratorConverted.hasNext()) {
            V converted = iteratorConverted.next();
            K val = iterator.next();

            if (tester.test(converted)) {
                if (converted.equals(converter.convertDelegate(val))) {
                    iterator.remove();
                    removed = true;
                } else {
                    removed = declaringCollection.removeIf(k -> converter.convertDelegate(k).equals(converted));
                }

                if (removed) {
                    iteratorConverted.remove();
                }
            }
        }
        return removed;
    }

    @Override
    public boolean contains(K o) {
        if (checkCollections()) return false;

        return declaringCollection.contains(o);
    }

    @Override
    public boolean containsConverted(V value) {
        return contains(converter.convertDeclaring(value));
    }

    @Override
    public boolean add(K value) {
        if (checkCollections()) return false;

        declaringCollection.add(value);
        return delegatedCollection.add(converter.convertDelegate(value));
    }

    @Override
    public boolean addConverted(V value) {
        return add(converter.convertDeclaring(value));
    }

    @Override
    public Iterator<V> iterator() {
        if (checkCollections()) return Collections.emptyIterator();

        return delegatedCollection.iterator();
    }
}
