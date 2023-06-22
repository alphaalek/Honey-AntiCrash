package me.alek.honey.netty.modelwrappers.nbt;

import me.alek.honey.netty.packet.structure.ReflectStructure;
import me.alek.honey.netty.packet.structure.converters.Converters;

import java.util.List;
import java.util.function.Function;

public class NBTList<T> extends NBTBase<List<Object>, List<T>> {

    private final NBTType typeOf;

    @Override
    public Function<ReflectStructure<Object, ?>, ReflectStructure<List<Object>, List<T>>> getValueStructure() {
        return (structure) -> structure.withType(getParameterizedClass(), Converters.getNBTListConverter());
    }

    @SuppressWarnings("unchecked")
    public <V> Class<List<V>> getParameterizedClass() {
        return (Class<List<V>>) (Class<?>) List.class;
    }

    public NBTList(Object base, NBTType typeOf) {
        super(base);
        this.typeOf = typeOf;
    }

    public int size() {
        return getValue().size();
    }

    public void clear() {
        getValue().clear();
    }

    public boolean isEmpty() {
        return getValue().isEmpty();
    }

    public NBTType getTypeOf() {
        return typeOf;
    }

    public T remove(int index) {
        return getValue().remove(index);
    }

    public boolean contains(T value) {
        return getValue().contains(value);
    }

    public T get(int index) {
        return getValue().get(index);
    }

    public void add(int index, T value) {
        getValue().set(index, value);
    }

}