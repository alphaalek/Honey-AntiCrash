package me.alek.honey.netty.modelwrappers.nbt.lists;

import me.alek.honey.netty.modelwrappers.nbt.*;

public class NBTList<T> extends NBTAbstractList<NBTElement<T>> {

    public NBTList(Object base, NBTType typeOf, NBTRoot root, NBTBase<?, ?> owner) {
        super(base, typeOf, root, owner);
    }

}