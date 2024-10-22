package me.alek.honey.netty.nbt.lists;

import me.alek.honey.netty.nbt.NBTAbstractList;
import me.alek.honey.netty.nbt.NBTBase;
import me.alek.honey.netty.nbt.NBTRoot;
import me.alek.honey.netty.nbt.NBTType;

public class NBTListOfList extends NBTAbstractList<NBTAbstractList<?>> {

    public NBTListOfList(Object base, NBTRoot root, NBTBase<?, ?> owner, boolean forceCreation) {
        super(base, NBTType.LIST, root, owner, forceCreation);
    }

    public NBTListOfList() {
        super(NBTType.LIST);
    }
}
