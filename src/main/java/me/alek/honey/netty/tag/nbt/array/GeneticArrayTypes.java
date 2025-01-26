package me.alek.honey.netty.tag.nbt.array;

import me.alek.honey.netty.tag.nbt.CompoundTag;
import me.alek.honey.netty.tag.nbt.TagType;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public class GeneticArrayTypes {

    public static class ArrayOfCompoundTag extends AbstractArrayTag<CompoundTag, ArrayOfCompoundTag, CompoundTag> {

        private static final TagType TYPE = TagType.getFromCollection(TagType.Collection.COMPOUND);

        public ArrayOfCompoundTag(Object base) {
            super(base, TYPE);
        }

        public ArrayOfCompoundTag() {
            super(TYPE);
        }

        public ArrayOfCompoundTag(Iterable<CompoundTag> array) {
            super(TYPE, array);
        }

        @NotNull
        @Override
        public Iterator<CompoundTag> iterator() {
            return getValues().iterator();
        }
    }

    public static class ArrayOfArrayTag<ARRAY extends ArrayOfArrayTag<ARRAY, IN>, IN extends AbstractArrayTag<?, IN, ?>> extends AbstractArrayTag<IN, ARRAY, IN> {

        private static final TagType TYPE = TagType.getFromCollection(TagType.Collection.ARRAY);

        public ArrayOfArrayTag(Object base) {
            super(base, TYPE);
        }

        public ArrayOfArrayTag() {
            super(TYPE);
        }

        public ArrayOfArrayTag(Iterable<IN> array) {
            super(TYPE, array);
        }

        @NotNull
        @Override
        public Iterator<IN> iterator() {
            return getValues().iterator();
        }
    }
}
