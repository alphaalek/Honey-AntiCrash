package me.alek.honey.netty.nbt;

import me.alek.honey.netty.structure.converters.JavaConverter;

public class TagConverter implements JavaConverter<Object, AbstractTag<?, ?, ?>> {

    private final boolean forceCreation;

    public TagConverter(boolean forceCreation) {
        this.forceCreation = forceCreation;
    }

    @Override
    public AbstractTag<?, ?, ?> convertDelegate(Object delegate, Object... params) {
        if (delegate == null) {
            return null;
        }
        return NBTFactory.fromDelegate(delegate, forceCreation);
    }

    @Override
    public Object convertDeclaring(AbstractTag<?, ?, ?> declaring) {
        if (declaring == null) {
            return null;
        }
        return declaring.getHandle();
    }
}
