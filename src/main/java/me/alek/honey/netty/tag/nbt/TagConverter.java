package me.alek.honey.netty.tag.nbt;

import me.alek.honey.netty.structure.converters.JavaConverter;

public class TagConverter implements JavaConverter<Object, AbstractTag<?, ?, ?>> {

    public static final TagConverter CONVERTER = new TagConverter();

    @Override
    public AbstractTag<?, ?, ?> convertDelegate(Object delegate, Object... params) {
        if (delegate == null) {
            return null;
        }
        return NBTFactory.fromDelegate(delegate);
    }

    @Override
    public Object convertDeclaring(AbstractTag<?, ?, ?> declaring) {
        if (declaring == null) {
            return null;
        }
        return declaring.getHandle();
    }
}
