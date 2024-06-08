package me.alek.honey.netty.structure.converters.dedicated;

import me.alek.honey.netty.modelwrappers.nbt.NBTBase;
import me.alek.honey.netty.modelwrappers.nbt.NBTFactory;
import me.alek.honey.netty.modelwrappers.nbt.NBTRoot;
import me.alek.honey.netty.structure.converters.JavaConverter;

public class NBTConverter implements JavaConverter<Object, NBTBase<?, ?>> {

    private final NBTRoot root;
    private final NBTBase<?, ?> owner;
    private final boolean forceCreation;

    public NBTConverter(NBTRoot root, NBTBase<?, ?> owner, boolean forceCreation) {
        this.root = root;
        this.owner = owner;
        this.forceCreation = forceCreation;
    }

    @Override
    public NBTBase<?, ?> convertDelegate(Object delegate, Object... params) {
        if (delegate == null) {
            return null;
        }
        return NBTFactory.fromDelegate(delegate, root, owner, forceCreation);
    }

    @Override
    public Object convertDeclaring(NBTBase<?, ?> declaring) {
        if (declaring == null) {
            return null;
        }
        return declaring.getHandle();
    }
}
