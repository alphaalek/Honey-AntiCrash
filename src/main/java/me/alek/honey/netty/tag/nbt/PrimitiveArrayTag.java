package me.alek.honey.netty.tag.nbt;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class PrimitiveArrayTag<T> extends ArrayTag<PrimitiveTag<T>, T, PrimitiveArrayTag<T>> {

    public PrimitiveArrayTag(TagType typeOf) {
        super(typeOf);
    }

    public PrimitiveArrayTag(TagType typeOf, Iterable<?> array) {
        super(typeOf,
                StreamSupport.stream(array.spliterator(), false)
                        .map(t -> NBTFactory.createTagOfExpectedType(typeOf, t))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
        );
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
}