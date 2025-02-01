package me.alek.honey.netty.structure.converters;

import org.jetbrains.annotations.NotNull;

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
        return this.declaringCollection == null || this.delegatedCollection == null;
    }

    protected boolean checkCollections() {
        if (this.declaringCollection == null) {
            return true;
        }

        int delegatedSize = this.delegatedCollection.size();
        int declaringSize = this.declaringCollection.size();
        if (delegatedSize != declaringSize) {
            this.delegatedCollection.clear();
            for (K declaring : this.declaringCollection) {
                this.delegatedCollection.add(this.converter.convertDelegate(declaring));
            }
        }

        return delegatedSize != declaringSize;
    }

    @Override
    public int size() {
        if (checkCollections()) return 0;

        return this.declaringCollection.size();
    }

    @Override
    public void clear() {
        if (checkCollections()) return;

        this.delegatedCollection.clear();
        this.declaringCollection.clear();
    }

    @Override
    public boolean isEmpty() {
        if (checkCollections()) return true;

        return this.declaringCollection.isEmpty();
    }

    @Override
    public boolean remove(K o) {
        if (checkCollections()) return false;

        this.declaringCollection.remove(o);
        return this.delegatedCollection.remove(this.converter.convertDelegate(o));
    }

    @Override
    public boolean removeConverted(V value) {
        if (checkCollections()) return false;

        this.declaringCollection.remove(this.converter.convertDeclaring(value));
        return this.delegatedCollection.remove(value);
    }

    @Override
    public boolean removeIfConverted(Predicate<V> tester) {
        Iterator<V> iteratorConverted = iterator();
        Iterator<K> iterator = this.declaringCollection.iterator();
        boolean removed = false;
        while (iteratorConverted.hasNext()) {
            V converted = iteratorConverted.next();
            K val = iterator.next();

            if (tester.test(converted)) {
                if (converted.equals(this.converter.convertDelegate(val))) {
                    iterator.remove();
                    removed = true;
                } else {
                    removed = this.declaringCollection.removeIf(k -> this.converter.convertDelegate(k).equals(converted));
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

        return this.declaringCollection.contains(o);
    }

    @Override
    public boolean containsConverted(V value) {
        return contains(this.converter.convertDeclaring(value));
    }

    @Override
    public boolean add(K value) {
        if (checkCollections()) return false;

        this.declaringCollection.add(value);
        return this.delegatedCollection.add(this.converter.convertDelegate(value));
    }

    @Override
    public boolean addConverted(V value) {
        return add(this.converter.convertDeclaring(value));
    }

    @Override
    public @NotNull Iterator<V> iterator() {
        if (checkCollections()) return Collections.emptyIterator();

        return this.delegatedCollection.iterator();
    }
}
