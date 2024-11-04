package me.alek.honey.netty.nbt.array;

import me.alek.honey.netty.nbt.AbstractTag;
import me.alek.honey.netty.nbt.CompoundTag;
import me.alek.honey.netty.nbt.TagType;

import java.util.List;

public class GeneticArrayTypes {

    public static class ArrayOfCompoundTag extends AbstractArrayTag<CompoundTag, ArrayOfCompoundTag> {

        public ArrayOfCompoundTag(Object base, boolean forceCreation) {
            super(base, TagType.getFromCollection(TagType.Collection.COMPOUND), forceCreation);
        }

        public ArrayOfCompoundTag() {
            super(TagType.getFromCollection(TagType.Collection.COMPOUND));
        }

        public ArrayOfCompoundTag(List<Object> array) {
            super(TagType.getFromCollection(TagType.Collection.COMPOUND), array);
        }
    }

    public static class ArrayOfArrayTag<T extends AbstractTag<?, ?, T>> extends AbstractArrayTag<ArrayTag<T>, ArrayOfArrayTag<T>> {

        public ArrayOfArrayTag(Object base, boolean forceCreation) {
            super(base, TagType.getFromCollection(TagType.Collection.ARRAY), forceCreation);
        }

        public ArrayOfArrayTag() {
            super(TagType.getFromCollection(TagType.Collection.ARRAY));
        }

        public ArrayOfArrayTag(List<Object> array) {
            super(TagType.getFromCollection(TagType.Collection.ARRAY), array);
        }
    }
}
