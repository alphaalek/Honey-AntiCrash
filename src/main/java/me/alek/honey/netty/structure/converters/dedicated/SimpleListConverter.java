package me.alek.honey.netty.structure.converters.dedicated;

import me.alek.honey.netty.structure.converters.AbstractCollectionConverter;
import me.alek.honey.netty.structure.converters.JavaConverter;
import me.alek.honey.netty.structure.converters.ListConverter;

import java.util.ArrayList;
import java.util.List;

public class SimpleListConverter<V, T> extends AbstractCollectionConverter<V, T> implements ListConverter<V, T> {

    private final boolean controlDeclaring;
    private final JavaConverter<V, T> converter;
    private final List<T> delegatedList = new ArrayList<>();
    private List<V> declaringList;

    public SimpleListConverter(JavaConverter<V, T> converter, boolean controlDeclaring) {
        super(converter);
        this.converter = converter;
        this.controlDeclaring = controlDeclaring;
    }

    public SimpleListConverter<V, T> apply(List<V> list) {
        applyCollections(list, delegatedList);

        if (controlDeclaring) {
            declaringList = list;
        }
        else {
            declaringList = new ArrayList<>();
        }
        for (V value : list) {
            if (!controlDeclaring) {
                declaringList.add(value);
            }
            delegatedList.add(converter.convertDelegate(value));
        }
        return this;
    }

    @Override
    public ListConverter<V, T> fakeApply(List<V> mapping) {
        return new SimpleListConverter<>(converter, false).apply(mapping);
    }

    public void clear() {
        if (checkCollections()) return;

        delegatedList.clear();
        declaringList.clear();
    }

    public boolean isEmpty() {
        if (checkCollections()) return true;

        return delegatedList.isEmpty();
    }

    public T removeConverted(int index) {
        if (checkCollections()) return null;

        return converter.convertDelegate(declaringList.remove(index));
    }

    public boolean removeConverted(T converted) {
        if (checkCollections()) return false;

        delegatedList.remove(converted);
        return declaringList.remove(converter.convertDeclaring(converted));
    }

    public T setConverted(int index, T value) {
        if (checkCollections()) return null;

        delegatedList.set(index, value);
        return converter.convertDelegate(declaringList.set(index, converter.convertDeclaring(value)));
    }

    public T getConverted(int index) {
        if (checkCollections()) return null;

        return delegatedList.get(index);
    }


    public List<T> getDelegatedList() {
        checkCollections();

        return delegatedList;
    }

    @Override
    public JavaConverter<V, T> getHandle() {
        return converter;
    }

    @Override
    public SimpleListConverter<V, T> convertDelegate(List<V> delegate, Object... params) {
        checkCollections();

        return this;
    }

    @Override
    public List<V> convertDeclaring(ListConverter<V, T> object) {
        if (checkCollections()) return new ArrayList<>();

        return declaringList;
    }
}
