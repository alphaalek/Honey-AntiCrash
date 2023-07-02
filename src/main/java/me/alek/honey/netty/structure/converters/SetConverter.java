package me.alek.honey.netty.structure.converters;

import java.util.Set;

public interface SetConverter<V, T> extends CollectionConverter<V, T>, JavaConverter<Set<V>, SetConverter<V, T>>, ApplicableConverter<Set<V>, SetConverter<V, T>> {

}
