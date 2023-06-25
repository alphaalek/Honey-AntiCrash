package me.alek.honey.netty.modelwrappers.nbt;

import me.alek.honey.netty.structure.converters.ListConverter;

import java.io.DataOutput;

public class NBTListInList extends NBTAbstractList<NBTList<?>> {
    public NBTListInList(Object base, ListConverter<Object, NBTList<?>> converter) {
        super(base, NBTType.LIST, converter);
    }

    @Override
    public NBTBase<?, ?> getUnknown(int index) {
        return getValue().getConverted(index);
    }

    @Override
    public void write(DataOutput output) throws Exception {
        output.writeByte((byte) getType().getTypeId());
        output.writeInt(size());
        for (NBTList<?> value : getValue().getDelegatedList()) {
            value.write(output);
        }
    }

}
