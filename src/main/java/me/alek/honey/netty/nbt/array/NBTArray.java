package me.alek.honey.netty.nbt.array;

import me.alek.honey.netty.nbt.NBTBase;
import me.alek.honey.netty.nbt.NBTElement;
import me.alek.honey.netty.nbt.NBTRoot;
import me.alek.honey.netty.nbt.NBTType;

public class NBTArray<T> extends NBTAbstractArray<NBTElement<T>> {

    public NBTArray(Object base, NBTType typeOf, NBTRoot root, NBTBase<?, ?> owner, boolean forceCreation) {
        super(base, typeOf, root, owner, forceCreation);
    }

    public NBTArray(NBTType typeOf) {
        super(typeOf);
    }
}