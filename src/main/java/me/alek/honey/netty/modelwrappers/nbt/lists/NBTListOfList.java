package me.alek.honey.netty.modelwrappers.nbt.lists;

import me.alek.honey.netty.modelwrappers.nbt.NBTAbstractList;
import me.alek.honey.netty.modelwrappers.nbt.NBTBase;
import me.alek.honey.netty.modelwrappers.nbt.NBTRoot;
import me.alek.honey.netty.modelwrappers.nbt.NBTType;

public class NBTListOfList extends NBTAbstractList<NBTAbstractList<?>> {

    public NBTListOfList(Object base, NBTRoot root, NBTBase<?, ?> owner) {
        super(base, NBTType.LIST, root, owner);
    }
}
