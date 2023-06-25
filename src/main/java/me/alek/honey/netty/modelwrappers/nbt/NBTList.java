package me.alek.honey.netty.modelwrappers.nbt;

import me.alek.honey.netty.structure.converters.ListConverter;

import java.io.DataOutput;

public class NBTList<T> extends NBTAbstractList<NBTBase<?, T>> {

    public NBTList(Object base, NBTType typeOf, ListConverter<Object, NBTBase<?, T>> converter) {
        super(base, typeOf, converter);
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