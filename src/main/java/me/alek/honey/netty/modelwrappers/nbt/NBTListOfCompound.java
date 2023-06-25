package me.alek.honey.netty.modelwrappers.nbt;

import me.alek.honey.netty.structure.converters.ListConverter;

import java.io.DataOutput;

public class NBTListOfCompound extends NBTAbstractList<NBTCompound> {

    public NBTListOfCompound(Object base, ListConverter<Object, NBTCompound> converter) {
        super(base, NBTType.COMPOUND, converter);
    }

    @Override
    public void write(DataOutput output) throws Exception {
        output.writeByte((byte) getType().getTypeId());
        output.writeInt(size());
        for (NBTCompound value : getValue().getDelegatedList()) {
            value.write(output);
        }
    }
}
