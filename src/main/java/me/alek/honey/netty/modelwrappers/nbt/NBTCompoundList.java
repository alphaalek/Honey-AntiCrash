package me.alek.honey.netty.modelwrappers.nbt;

import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.converters.ApplicableConverter;
import me.alek.honey.netty.structure.converters.ListConverter;

import java.io.DataOutput;
import java.util.List;
import java.util.function.Function;

public class NBTCompoundList extends NBTAbstractList<NBTCompound> {

    public NBTCompoundList(Object base, ListConverter<Object, NBTCompound> converter) {
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

    @Override
    public NBTBase<?, ?> getUnknown(int index) {
        return getValue().getConverted(index);
    }
}
