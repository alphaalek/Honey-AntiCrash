package me.alek.honey.netty.modelwrappers.nbt.lists;

import me.alek.honey.netty.modelwrappers.nbt.*;

public class NBTListOfCompound extends NBTAbstractList<NBTCompound> {

    public NBTListOfCompound(Object base, NBTRoot root, NBTBase<?, ?> owner, boolean forceCreation) {
        super(base, NBTType.COMPOUND, root, owner, forceCreation);
    }

    public NBTListOfCompound() {
        super(NBTType.COMPOUND);
    }
}
