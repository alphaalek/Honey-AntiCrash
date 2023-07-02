package me.alek.honey.netty.structure.converters.dedicated;

import me.alek.honey.netty.structure.converters.AbstractCollectionConverter;
import me.alek.honey.netty.structure.converters.ApplicableConverter;
import me.alek.honey.netty.structure.converters.JavaConverter;
import me.alek.honey.netty.structure.converters.SetConverter;

import java.util.HashSet;
import java.util.Set;

public class SimpleSetConverter<K, V> extends AbstractCollectionConverter<K, V> implements SetConverter<K, V> {

    private final JavaConverter<K, V> converter;
    private final boolean controlDeclaring;
    private final Set<V> delegatedSet = new HashSet<>();
    private Set<K> declaringSet;

    public SimpleSetConverter(JavaConverter<K, V> converter, boolean controlDeclaring) {
        super(converter);
        this.converter = converter;
        this.controlDeclaring = controlDeclaring;
    }

    @Override
    public ApplicableConverter<Set<K>, SetConverter<K, V>> apply(Set<K> mapping) {
        applyCollections(mapping, delegatedSet);

        if (controlDeclaring) {
            declaringSet = mapping;
        }
        else {
            declaringSet = new HashSet<>();
        }
        for (K value : mapping) {
            if (!controlDeclaring) {
                declaringSet.add(value);
            }
            delegatedSet.add(converter.convertDelegate(value));
        }
        return this;
    }

    @Override
    public SetConverter<K, V> fakeApply(Set<K> mapping) {
        return (SetConverter<K, V>) new SimpleSetConverter<>(converter, false).apply(mapping);
    }

    @Override
    public SetConverter<K, V> convertDelegate(Set<K> delegate, Object... params) {
        return this;
    }

    @Override
    public Set<K> convertDeclaring(SetConverter<K, V> object) {
        return declaringSet;
    }
}
