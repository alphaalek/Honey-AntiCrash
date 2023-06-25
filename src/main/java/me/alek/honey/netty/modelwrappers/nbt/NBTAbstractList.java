package me.alek.honey.netty.modelwrappers.nbt;

import me.alek.honey.netty.structure.converters.ListConverter;

import java.util.Iterator;
import java.util.List;

public abstract class NBTAbstractList<T extends NBTBase<?, ?>> extends NBTAbstractListWrapper<Object, T> implements Iterable<T> {

    private final NBTType typeOf;

    public NBTAbstractList(Object base, NBTType typeOf, ListConverter<Object, T> converter) {
        super(base, converter);
        this.typeOf = typeOf;
    }

    @Override
    public Iterator<T> iterator() {
        return getValue().getDelegatedList().iterator();
    }

    @Override
    public NBTList<T> asList() {
        if (typeOf == NBTType.LIST || typeOf == NBTType.COMPOUND || typeOf == NBTType.LISTOFCOMOUND || typeOf == NBTType.LISTOFLIST) {
            return null;
        }
        return (NBTList<T>) this;
    }

    public <R> NBTList<R> asListOf(Class<R> clazz) {
        if (typeOf == NBTType.LIST || typeOf == NBTType.COMPOUND || typeOf == NBTType.LISTOFCOMOUND || typeOf == NBTType.LISTOFLIST) {
            return null;
        }
        return (NBTList<R>) this;
    }


    public NBTListOfList asListOfList() {
        if (typeOf != NBTType.LIST) {
            return null;
        }
        return (NBTListOfList) this;
    }

    public NBTListOfCompound asListOfCompound() {
        if (typeOf != NBTType.LIST) {
            return null;
        }
        return (NBTListOfCompound) this;
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
        return getValue().removeConverted(index);
    }

    public boolean remove(T value) {
        return getValue().removeConverted(value);
    }

    public boolean contains(T value) {
        return getValue().containsConverted(value);
    }

    public T get(int index) {
        return getValue().getConverted(index);
    }

    public T set(int index, T value) {
        return getValue().setConverted(index, value);
    }

    public List<T> getValues() {
        return getValue().getDelegatedList();
    }

    public boolean add(T value) {
        return getValue().addConverted(value);
    }
}
