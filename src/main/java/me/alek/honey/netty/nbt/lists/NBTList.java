package me.alek.honey.netty.nbt.lists;

import me.alek.honey.netty.nbt.*;

public class NBTList<T> extends NBTAbstractList<NBTElement<T>> {

    public NBTList(Object base, NBTType typeOf, NBTRoot root, NBTBase<?, ?> owner, boolean forceCreation) {
        super(base, typeOf, root, owner, forceCreation);
    }

    public NBTList(NBTType typeOf) {
        super(typeOf);
    }
}