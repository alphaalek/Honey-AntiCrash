package me.alek.honey.netty.nbt.array;

import me.alek.honey.netty.nbt.NBTBase;
import me.alek.honey.netty.nbt.NBTCompound;
import me.alek.honey.netty.nbt.NBTRoot;
import me.alek.honey.netty.nbt.NBTType;

import java.util.List;

public class GeneticArrayTypes {

    public static class NBTArrayOfCompound extends NBTAbstractArray<NBTCompound> {

        public NBTArrayOfCompound(Object base, NBTRoot root, NBTBase<?, ?> owner, boolean forceCreation) {
            super(base, NBTType.COMPOUND, root, owner, forceCreation);
        }

        public NBTArrayOfCompound() {
            super(NBTType.COMPOUND);
        }

        public NBTArrayOfCompound(List<?> array) {
            super(NBTType.COMPOUND, array);
        }
    }

    public static class NBTArrayOfArray extends NBTAbstractArray<NBTAbstractArray<?>> {

        public NBTArrayOfArray(Object base, NBTRoot root, NBTBase<?, ?> owner, boolean forceCreation) {
            super(base, NBTType.ARRAY, root, owner, forceCreation);
        }

        public NBTArrayOfArray() {
            super(NBTType.ARRAY);
        }

        public NBTArrayOfArray(List<?> array) {
            super(NBTType.ARRAY, array);
        }
    }
}
