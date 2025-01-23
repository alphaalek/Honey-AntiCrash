package me.alek.honey.netty.tag.nbt.array;

import me.alek.honey.netty.tag.nbt.AbstractTag;
import me.alek.honey.netty.tag.nbt.CompoundTag;
import me.alek.honey.netty.tag.nbt.TagType;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;

public class GeneticArrayTypes {

    public static class ArrayOfCompoundTag extends AbstractArrayTag<CompoundTag, ArrayOfCompoundTag, CompoundTag> {

        public ArrayOfCompoundTag(Object base, boolean forceCreation) {
            super(base, TagType.getFromCollection(TagType.Collection.COMPOUND), forceCreation);
        }

        public ArrayOfCompoundTag() {
            super(TagType.getFromCollection(TagType.Collection.COMPOUND));
        }

        public ArrayOfCompoundTag(List<Object> array) {
            super(TagType.getFromCollection(TagType.Collection.COMPOUND), array);
        }

        @NotNull
        @Override
        public Iterator<CompoundTag> iterator() {
            return getValues().iterator();
        }
    }

    public static class ArrayOfArrayTag<ARRAY extends ArrayOfArrayTag<ARRAY, IN>, IN extends AbstractArrayTag<?, IN, ?>> extends AbstractArrayTag<IN, ARRAY, IN> {

        public ArrayOfArrayTag(Object base, boolean forceCreation) {
            super(base, TagType.getFromCollection(TagType.Collection.ARRAY), forceCreation);
        }

        public ArrayOfArrayTag() {
            super(TagType.getFromCollection(TagType.Collection.ARRAY));
        }

        public ArrayOfArrayTag(List<Object> array) {
            super(TagType.getFromCollection(TagType.Collection.ARRAY), array);
        }

        @NotNull
        @Override
        public Iterator<IN> iterator() {
            return getValues().iterator();
        }
    }
}
