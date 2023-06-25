package me.alek.honey.netty.modelwrappers.nbt;

import me.alek.honey.netty.structure.converters.ListConverter;

import java.io.DataOutput;
import java.util.Iterator;

public class NBTListOfList extends NBTAbstractList<NBTAbstractList<?>> {

    public NBTListOfList(Object base, ListConverter<Object, NBTAbstractList<?>> converter) {
        super(base, NBTType.LIST, converter);
    }

    @Override
    public void write(DataOutput output) throws Exception {
        output.writeByte((byte) getType().getTypeId());
        output.writeInt(size());
        for (NBTAbstractList<?> value : getValue().getDelegatedList()) {
            value.write(output);
        }
    }
}
