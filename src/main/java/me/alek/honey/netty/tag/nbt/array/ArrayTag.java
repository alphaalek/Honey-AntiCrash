package me.alek.honey.netty.tag.nbt.array;

import me.alek.honey.netty.tag.nbt.PrimitiveTag;
import me.alek.honey.netty.tag.nbt.TagType;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;

public class ArrayTag<T> extends AbstractArrayTag<PrimitiveTag<T>, ArrayTag<T>, T> {

    public ArrayTag(Object base, TagType typeOf) {
        super(base, typeOf);
    }

    public ArrayTag(TagType typeOf) {
        super(typeOf);
    }

    public ArrayTag(TagType typeOf, Iterable<T> array) {
        super(typeOf, array);
    }

    @Override
    public T from(PrimitiveTag<T> tag) {
        return tag.getValue();
    }

    @SuppressWarnings("unchecked")
    @Override
    public PrimitiveTag<T> to(T val) {
        if (val instanceof PrimitiveTag) {
            return (PrimitiveTag<T>) val;
        } else {
            return new PrimitiveTag<>(this.typeOf, val);
        }
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return getValues().stream().map(PrimitiveTag::getValue).iterator();
    }
}