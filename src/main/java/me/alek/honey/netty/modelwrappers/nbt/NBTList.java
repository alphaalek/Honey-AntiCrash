package me.alek.honey.netty.modelwrappers.nbt;

import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.converters.ApplicableConverter;
import me.alek.honey.netty.structure.converters.ListConverter;

import java.io.DataOutput;
import java.util.List;
import java.util.function.Function;

public class NBTList<T> extends NBTAbstractList<NBTBase<?, T>> {

    public NBTList(Object base, NBTType typeOf, ListConverter<Object, NBTBase<?, T>> converter) {
        super(base, typeOf, converter);
    }

    @Override
    public Function<ReflectStructure<Object, ?>, ReflectStructure<List<Object>, ListConverter<Object, NBTBase<?, T>>>> getValueStructure(ApplicableConverter<List<Object>, ListConverter<Object, NBTBase<?, T>>> converter, List<Object> original) {
        return (structure) -> structure.withSpecificType(List.class, converter.apply(original), false);
    }

    @Override
    public void write(DataOutput output) throws Exception {
        output.writeByte((byte) getType().getTypeId());
        output.writeInt(size());
        for (NBTBase<?, T> value : getValue().getDelegatedList()) {
            value.write(output);
        }
    }

}