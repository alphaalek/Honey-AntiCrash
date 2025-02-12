package me.alek.honey.netty.tag.nbt.array;

import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.structure.converters.ListConverter;
import me.alek.honey.netty.structure.converters.MapConverter;
import me.alek.honey.netty.tag.nbt.*;
import me.alek.honey.util.LazyInit;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Map;
import java.util.stream.StreamSupport;

public class GeneticArrayTypes {

    public static class ArrayOfCompoundTag extends AbstractArrayTag<CompoundTag, ArrayOfCompoundTag, MapConverter<String, Object, AbstractTag<?, ?, ?>>> {

        public static final LazyInit<TagType> TYPE = new LazyInit<>(() -> TagTypeCollection.getFromCollection(TagTypeCollection.ARRAY_OF_COMPOUND));

        public ArrayOfCompoundTag(Object base) {
            super(base, TYPE.get());
        }

        public ArrayOfCompoundTag() {
            super(TYPE.get());
        }

        @SuppressWarnings("unchecked")
        public ArrayOfCompoundTag(Iterable<?> array) {
            super(TYPE.get(), Converters.getNBTListConverter());
            for (Object entry : array) {
                if (entry instanceof CompoundTag) {
                    getValue().addConverted((CompoundTag) entry);
                } else if (entry instanceof Map) {
                    getValue().addConverted(new CompoundTag((Map<String, Object>) entry));
                }
            }
        }

        @Override
        public MapConverter<String, Object, AbstractTag<?, ?, ?>> from(CompoundTag tag) {
            return tag.getValue();
        }

        @Override
        public CompoundTag to(MapConverter<String, Object, AbstractTag<?, ?, ?>> val) {
            if (val instanceof CompoundTag) {
                return (CompoundTag) val;
            } else {
                return new CompoundTag(val.getDelegatedMap());
            }
        }

        @NotNull
        @Override
        public Iterator<MapConverter<String, Object, AbstractTag<?, ?, ?>>> iterator() {
            return getValues().stream().map(CompoundTag::getValue).iterator();
        }
    }

    public static class ArrayOfArrayTag<ARRAY extends ArrayOfArrayTag<ARRAY, IN, IN_IN>, IN extends AbstractArrayTag<IN_IN, IN, ?>, IN_IN extends AbstractTag<?, ?, IN_IN>> extends AbstractArrayTag<IN, ARRAY, ListConverter<Object, IN_IN>> {

        public static final LazyInit<TagType> TYPE = new LazyInit<>(() -> TagTypeCollection.getFromCollection(TagTypeCollection.ARRAY_OF_ARRAY));

        public ArrayOfArrayTag(Object base) {
            super(base, TYPE.get());
        }

        public ArrayOfArrayTag() {
            super(TYPE.get());
        }

        @SuppressWarnings("unchecked")
        public ArrayOfArrayTag(Iterable<?> array) {
            super(TYPE.get(), Converters.getNBTListConverter());
            for (Object entry : array) {
                getValue().addConverted((IN) NBTFactory.createTag(entry));
            }
        }

        @Override
        public ListConverter<Object, IN_IN> from(IN tag) {
            return tag.getValue();
        }

        @SuppressWarnings("unchecked")
        @Override
        public IN to(ListConverter<Object, IN_IN> val) {
            return (IN) NBTFactory.createTag(val.getDelegatedList());
        }

        @NotNull
        @Override
        public Iterator<ListConverter<Object, IN_IN>> iterator() {
            return StreamSupport.stream(getValue().spliterator(), false).map(AbstractTag::getValue).iterator();
        }
    }
}
