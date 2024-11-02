package me.alek.honey.netty.nbt.array;

import me.alek.honey.netty.nbt.AbstractTag;
import me.alek.honey.netty.nbt.TagType;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.converters.ApplicableConverter;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.structure.converters.ListConverter;
import org.bukkit.Bukkit;

import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public abstract class AbstractArrayTag<T extends AbstractTag<?, ?>> extends AbstractTag<List<Object>, ListConverter<Object, T>> implements Iterable<T> {

    private static final TagType TYPE = TagType.getFromCollection(TagType.Collection.ARRAY);

    private ListConverter<Object, T> delegate;
    private final TagType typeOf;

    public AbstractArrayTag(Object base, TagType typeOf, boolean forceCreation) {
        super(base, forceCreation);
        this.typeOf = typeOf;
    }

    public AbstractArrayTag(TagType typeOf) {
        this(typeOf, new ArrayList<>());
    }

    public AbstractArrayTag(TagType typeOf, List<Object> array) {
        super(createBaseFromArray(array, typeOf.getTypeId()));
        this.typeOf = typeOf;

        if (TYPE.equals(TagType.ARRAY_LEGACY)) {
            for (Object val : array) {
                getValue().add(val);
            }
            ReflectStructure.Provider.acquireStructure(TYPE.getDelegateClass())
                    .withBytes().withTarget(getHandle()).writeField(0, this.typeOf.getTypeId());

            ReflectStructure.debug(getHandle());
        }
    }

    private static Object createBaseFromArray(List<?> array, byte typeId) {
        if (TYPE.equals(TagType.ARRAY_LEGACY)) {
            return TYPE.createRawDelegate();
        } else if (TYPE.equals(TagType.ARRAY_MODERN)) {
            return TYPE.getBaseInvoker().invoke(array, typeId);
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public void write(DataOutput output) throws IOException {
        getType().getCodec().write(output, this);
    }

    @Override
    public Iterator<T> iterator() {
        return getValue().getDelegatedList().iterator();
    }

    public Stream<T> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    public <E> Stream<E> streamOfValues() {
        return stream().map(T::getValue).map(v -> (E) v);
    }

    @Override
    public ApplicableConverter<List<Object>, ListConverter<Object, T>> getNBTConverter() {
        if (this.delegate == null) {
            this.delegate = Converters.getNBTListConverter(this.forceCreateChildren);
        }
        return this.delegate;
    }

    @Override
    public Function<ReflectStructure<Object, ?>, ReflectStructure<List<Object>, ListConverter<Object, T>>> applyStructure(ApplicableConverter<List<Object>, ListConverter<Object, T>> converter) {
        return (structure) -> structure.withSpecificType(List.class, converter, false);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ArrayTag<T> asArray() {
        if (typeOf.getCollection() == TagType.Collection.ARRAY
                || typeOf.getCollection() == TagType.Collection.COMPOUND) {
            return null;
        }

        return (ArrayTag<T>) this;
    }

    @SuppressWarnings("unchecked")
    public <R> ArrayTag<R> asArrayOf(Class<R> clazz) {
        if (typeOf.getCollection() == TagType.Collection.ARRAY
                || typeOf.getCollection() == TagType.Collection.COMPOUND) {
            return null;
        }

        return (ArrayTag<R>) this;
    }


    public GeneticArrayTypes.ArrayOfArrayTag asArrayOfArray() {
        if (typeOf.getCollection() != TagType.Collection.ARRAY) {
            return null;
        }
        return (GeneticArrayTypes.ArrayOfArrayTag) this;
    }

    public GeneticArrayTypes.ArrayOfCompoundTag asArrayOfCompound() {
        if (typeOf.getCollection() != TagType.Collection.COMPOUND) {
            return null;
        }
        return (GeneticArrayTypes.ArrayOfCompoundTag) this;
    }

    public int size() {
        return getValue().size();
    }

    public void clear() {
        getValue().clear();
    }

    public boolean isEmpty() {
        return getValue().isEmpty();
    }

    public TagType getTypeOf() {
        return typeOf;
    }

    public T remove(int index) {
        return getValue().removeConverted(index);
    }

    public boolean remove(T value) {
        return getValue().removeConverted(value);
    }

    public boolean contains(T value) {
        return getValue().containsConverted(value);
    }

    public T get(int index) {
        return getValue().getConverted(index);
    }

    public T set(int index, T value) {
        if (value.getType() != typeOf) {
            return null;
        }
        return getValue().setConverted(index, value);
    }

    public boolean add(T value) {
        if (value.getType() != typeOf) {
            return false;
        }
        return getValue().addConverted(value);
    }

    public List<T> getValues() {
        return getValue().getDelegatedList();
    }
}
