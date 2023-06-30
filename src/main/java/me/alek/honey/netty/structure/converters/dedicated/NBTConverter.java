package me.alek.honey.netty.structure.converters.dedicated;

import me.alek.honey.netty.modelwrappers.nbt.NBTBase;
import me.alek.honey.netty.modelwrappers.nbt.NBTFactory;
import me.alek.honey.netty.structure.converters.JavaConverter;

public class NBTConverter implements JavaConverter<Object, NBTBase<?, ?>> {

    private NBTBase<?, ?> owner;

    public NBTConverter(NBTBase<?, ?> owner) {
        this.owner = owner;
    }

    public void setOwner(NBTBase<?, ?> owner) {
        this.owner = owner;
    }

    @Override
    public NBTBase<?, ?> convertDelegate(Object delegate, Object... params) {
        if (delegate == null) {
            return null;
        }
        return NBTFactory.fromDelegate(delegate, owner);
    }

    @Override
    public Object convertDeclaring(NBTBase<?, ?> declaring) {
        if (declaring == null) {
            return null;
        }
        return declaring.getHandle();
    }
}
