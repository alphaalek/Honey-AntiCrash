package me.alek.honey.netty.nbt.array;

import me.alek.honey.netty.nbt.*;

public class GeneticArrayTypes {

    public static class NBTArrayOfCompound extends NBTAbstractArray<NBTCompound> {

        public NBTArrayOfCompound(Object base, NBTRoot root, NBTBase<?, ?> owner, boolean forceCreation) {
            super(base, NBTType.COMPOUND, root, owner, forceCreation);
        }

        public NBTArrayOfCompound() {
            super(NBTType.COMPOUND);
        }
    }

    public static class NBTArrayOfArray extends NBTAbstractArray<NBTAbstractArray<?>> {

        public NBTArrayOfArray(Object base, NBTRoot root, NBTBase<?, ?> owner, boolean forceCreation) {
            super(base, NBTType.ARRAY, root, owner, forceCreation);
        }

        public NBTArrayOfArray() {
            super(NBTType.ARRAY);
        }
    }
}
