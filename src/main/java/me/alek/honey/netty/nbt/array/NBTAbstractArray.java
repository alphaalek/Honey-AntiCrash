package me.alek.honey.netty.nbt.array;

import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.nbt.NBTBase;
import me.alek.honey.netty.nbt.NBTRoot;
import me.alek.honey.netty.nbt.NBTType;
import me.alek.honey.netty.nbt.array.GeneticArrayTypes;
import me.alek.honey.netty.nbt.array.NBTArray;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.converters.ApplicableConverter;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.structure.converters.JavaConverter;
import me.alek.honey.netty.structure.converters.ListConverter;

import java.io.DataOutput;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public abstract class NBTAbstractArray<T extends NBTBase<?, ?>> extends NBTBase<List<Object>, ListConverter<Object, T>>implements Iterable<T> {

    private ListConverter<Object, T> delegate;
    private final NBTType typeOf;

    public NBTAbstractArray(Object base, NBTType typeOf, NBTRoot root, NBTBase<?, ?> owner, boolean forceCreation) {
        super(base, root, owner, forceCreation);
        this.typeOf = typeOf;
    }

    public NBTAbstractArray(NBTType typeOf) {
        super(NBTType.ARRAY.create());
        this.typeOf = typeOf;
    }

    @Override
    public Iterator<T> iterator() {
        return getValue().getDelegatedList().iterator();
    }

    @Override
    public ApplicableConverter<List<Object>, ListConverter<Object, T>> getConverter() {
        if (delegate == null) {
            delegate = Converters.getNBTListConverter(getRoot(), this, forceCreateChildren);
        }
        return delegate;
    }

    @Override
    public void write(DataOutput output) throws Exception {
        output.writeByte((byte) getType().getTypeId());
        output.writeInt(size());
        for (final T value : getValue().getDelegatedList()) {
            value.write(output);
        }
    }

    @Override
    public Function<ReflectStructure<Object, ?>, ReflectStructure<List<Object>, ListConverter<Object, T>>> applyStructure(ApplicableConverter<List<Object>, ListConverter<Object, T>> converter) {
        return (structure) -> structure.withSpecificType(List.class, converter, false);
    }

    public JavaConverter<Object, T> getConverterHandle() {
        return delegate.getHandle();
    }

    @Override
    public NBTArray<T> asList() {
        if (typeOf == NBTType.ARRAY || typeOf == NBTType.COMPOUND || typeOf == NBTType.ARRAY_OF_COMPOUND || typeOf == NBTType.ARRAY_OF_ARRAY) {
            return null;
        }
        return (NBTArray<T>) this;
    }

    public <R> NBTArray<R> asListOf(Class<R> clazz) {
        if (typeOf == NBTType.ARRAY || typeOf == NBTType.COMPOUND || typeOf == NBTType.ARRAY_OF_COMPOUND || typeOf == NBTType.ARRAY_OF_ARRAY) {
            return null;
        }
        return (NBTArray<R>) this;
    }


    public GeneticArrayTypes.NBTArrayOfArray asListOfList() {
        if (typeOf != NBTType.ARRAY) {
            return null;
        }
        return (GeneticArrayTypes.NBTArrayOfArray) this;
    }

    public GeneticArrayTypes.NBTArrayOfCompound asListOfCompound() {
        if (typeOf != NBTType.COMPOUND) {
            return null;
        }
        return (GeneticArrayTypes.NBTArrayOfCompound) this;
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
        T val = getValue().removeConverted(index);
        deleteInherited(val);
        return val;
    }

    public boolean remove(T value) {
        boolean present = getValue().removeConverted(value);
        if (present) {
            deleteInherited(value);
        }
        return present;
    }

    public boolean contains(T value) {
        return getValue().containsConverted(value);
    }

    public T get(int index) {
        return getValue().getConverted(index);
    }

    public T set(int index, T value) {
        setInherited(value);
        return getValue().setConverted(index, value);
    }

    public boolean add(T value) {
        setInherited(value);
        return getValue().addConverted(value);
    }

    public List<T> getValues() {
        return getValue().getDelegatedList();
    }

    private void setInherited(T value) {
        value.setOwner(this);
        value.setRoot(getRoot());
    }

    private void deleteInherited(T value) {
        value.setRoot(NBTRoot.createNewDefault());
        value.setOwner(null);
    }
}
