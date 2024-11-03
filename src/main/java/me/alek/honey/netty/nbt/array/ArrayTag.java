package me.alek.honey.netty.nbt.array;

import me.alek.honey.netty.nbt.PrimitiveTag;
import me.alek.honey.netty.nbt.TagType;

import java.util.List;

public class ArrayTag<T> extends AbstractArrayTag<PrimitiveTag<T>, ArrayTag<T>> {

    public ArrayTag(Object base, TagType typeOf, boolean forceCreation) {
        super(base, typeOf, forceCreation);
    }

    public ArrayTag(TagType typeOf) {
        super(typeOf);
    }

    public ArrayTag(List<Object> array, TagType typeOf) {
        super(typeOf, array);
    }
}