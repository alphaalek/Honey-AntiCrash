package me.alek.honey.netty.modelwrappers.nbt.lists;

import me.alek.honey.netty.modelwrappers.nbt.*;

public class NBTList<T> extends NBTAbstractList<NBTElement<T>> {

    public NBTList(Object base, NBTType typeOf, NBTRoot root, NBTBase<?, ?> owner, boolean forceCreation) {
        super(base, typeOf, root, owner, forceCreation);
    }

    public NBTList(NBTType typeOf) {
        super(typeOf);
    }
}