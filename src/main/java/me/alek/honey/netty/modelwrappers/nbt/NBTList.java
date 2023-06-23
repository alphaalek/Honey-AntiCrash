package me.alek.honey.netty.modelwrappers.nbt;

import me.alek.honey.netty.packet.structure.ReflectStructure;
import me.alek.honey.netty.packet.structure.converters.Converters;
import me.alek.honey.netty.packet.structure.converters.ListConverterWrapper;
import me.alek.honey.netty.packet.structure.converters.JavaConverter;

import java.io.DataOutput;
import java.util.List;
import java.util.function.Function;

public class NBTList<T> extends NBTBase<List<Object>, ListConverterWrapper<Object, NBTBase<?, T>>> {

    private final ListConverterWrapper<Object, NBTBase<?, T>> CONVERTER = Converters.getNBTListConverter();
    private final NBTType typeOf;

    @Override
    @SuppressWarnings("unchecked")
    public Function<ReflectStructure<Object, ?>, ReflectStructure<List<Object>, ListConverterWrapper<Object, NBTBase<?, T>>>> getValueStructure(List<Object> original) {
        return (structure) -> structure.withSpecificType(List.class, (JavaConverter<List<Object>, ListConverterWrapper<Object, NBTBase<?, T>>>) (JavaConverter<?, ?>) CONVERTER.apply(original));
    }

    @Override
    public void write(DataOutput output) throws Exception {
        output.writeByte((byte) getType().getTypeId());
        output.writeInt(size());
        for (NBTBase<?, T> value : getValue().getDelegatedList()) {
            value.write(output);
        }
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

    public NBTBase<?, T> remove(int index) {
        return getValue().removeConverted(index);
    }

    public boolean remove(NBTBase<?, T> value) {
        return getValue().removeConverted(value);
    }

    public boolean contains(NBTBase<?, T> value) {
        return getValue().containsConverted(value);
    }

    public NBTBase<?, T> get(int index) {
        return getValue().getConverted(index);
    }

    public NBTBase<?, T> set(int index, NBTBase<?, T> value) {
        return getValue().setConverted(index, value);
    }

    public List<NBTBase<?, T>> getValues() {
        return getValue().getDelegatedList();
    }

    public boolean add(NBTBase<?, T> value) {
        return getValue().addConverted(value);
    }

}