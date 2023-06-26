package me.alek.honey.netty.modelwrappers.nbt;

import me.alek.honey.netty.structure.converters.ListConverter;

import java.io.DataOutput;

public class NBTListOfCompound extends NBTAbstractList<NBTCompound> {

    public NBTListOfCompound(Object base, NBTRoot root, NBTBase<?, ?> owner) {
        super(base, NBTType.COMPOUND, root, owner);
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
