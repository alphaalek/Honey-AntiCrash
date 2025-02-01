package me.alek.honey.netty.structure.converters.wrapper;

import me.alek.honey.netty.structure.converters.AbstractCollectionConverter;
import me.alek.honey.netty.structure.converters.JavaConverter;
import me.alek.honey.netty.structure.converters.ListConverter;

import java.util.ArrayList;
import java.util.List;

public class SimpleListConverter<V, T> extends AbstractCollectionConverter<V, T> implements ListConverter<V, T> {

    private final JavaConverter<V, T> converter;
    private final List<T> delegatedList = new ArrayList<>();
    private List<V> declaringList;

    public SimpleListConverter(JavaConverter<V, T> converter) {
        super(converter);
        this.converter = converter;
    }

    public SimpleListConverter<V, T> inject(List<V> list) {
        applyCollections(list, this.delegatedList);

        this.declaringList = list;
        for (V value : list) {
            this.delegatedList.add(this.converter.convertDelegate(value));
        }
        return this;
    }

    public void clear() {
        if (checkCollections()) return;

        this.delegatedList.clear();
        this.declaringList.clear();
    }

    public boolean isEmpty() {
        if (checkCollections()) return true;

        return this.delegatedList.isEmpty();
    }

    public T removeConverted(int index) {
        if (checkCollections()) return null;

        return this.converter.convertDelegate(this.declaringList.remove(index));
    }

    public boolean removeConverted(T converted) {
        if (checkCollections()) return false;

        this.delegatedList.remove(converted);
        return this.declaringList.remove(this.converter.convertDeclaring(converted));
    }

    public T setConverted(int index, T value) {
        if (checkCollections()) return null;

        this.delegatedList.set(index, value);
        return this.converter.convertDelegate(this.declaringList.set(index, this.converter.convertDeclaring(value)));
    }

    public T getConverted(int index) {
        if (checkCollections()) return null;

        return this.delegatedList.get(index);
    }

    @Override
    public T getRaw(int index) {
        return this.delegatedList.get(index);
    }

    public List<T> getDelegatedList() {
        checkCollections();

        return this.delegatedList;
    }

    @Override
    public JavaConverter<V, T> getHandle() {
        return this.converter;
    }

    @Override
    public SimpleListConverter<V, T> convertDelegate(List<V> delegate, Object... params) {
        checkCollections();

        return this;
    }

    @Override
    public List<V> convertDeclaring(ListConverter<V, T> object) {
        if (checkCollections()) return new ArrayList<>();

        return this.declaringList;
    }
}
