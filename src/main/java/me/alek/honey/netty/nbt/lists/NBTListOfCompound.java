package me.alek.honey.netty.nbt.lists;

import me.alek.honey.netty.nbt.*;

public class NBTListOfCompound extends NBTAbstractList<NBTCompound> {

    public NBTListOfCompound(Object base, NBTRoot root, NBTBase<?, ?> owner, boolean forceCreation) {
        super(base, NBTType.COMPOUND, root, owner, forceCreation);
    }

    public NBTListOfCompound() {
        super(NBTType.COMPOUND);
    }
}
