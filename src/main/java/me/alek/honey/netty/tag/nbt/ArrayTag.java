package me.alek.honey.netty.tag.nbt;

import me.alek.honey.util.LazyInit;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ArrayTag<IN extends AbstractTag<?, IN>, IN_F, ARRAY extends ArrayTag<IN, IN_F, ARRAY>> extends AbstractTag<List<IN_F>, ARRAY> implements Iterable<IN_F> {

    public static final LazyInit<TagType> TYPE = new LazyInit<>(() -> TagTypeCollection.getFromCollection(TagTypeCollection.ARRAY));

    private static final List<AbstractTag<?, ?>> EMPTY = new ArrayList<>();
    private final List<IN> delegate = new ArrayList<>();
    protected final TagType typeOf;

    public ArrayTag(TagType typeOf) {
        this(typeOf, EMPTY);
    }

    @SuppressWarnings("unchecked")
    public ArrayTag(TagType typeOf, Iterable<?> array) {
        super(TYPE.get());
        this.typeOf = typeOf;

        this.delegate.addAll(
                StreamSupport.stream(array.spliterator(), false)
                        .map(v -> (IN) NBTFactory.createTagOfExpectedType(this.typeOf, v))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
        );
    }

    @Override
    public String toString() {
        return toString("");
    }

    public String toString(String emptyKey) {
        String inner = this.delegate.stream()
                .map(tag -> (tag instanceof CompoundTag ? ((CompoundTag) tag).toString(emptyKey) : tag.toString()))
                .collect(Collectors.joining(","));
        return "[" + inner + "]";
    }

    @SuppressWarnings("unchecked")
    public IN_F from(IN in) {
        // to be overridden
        return (IN_F) in;
    }

    @SuppressWarnings("unchecked")
    public IN to(IN_F in) {
        // to be overridden
        return (IN) in;
    }

    @SuppressWarnings("unchecked")
    public PrimitiveArrayTag<IN> asPrimitiveArray() {
        return (PrimitiveArrayTag<IN>) this;
    }

    @SuppressWarnings("unchecked")
    public <R> PrimitiveArrayTag<R> asPrimitiveArrayOf(Class<R> clazz) {
        return (PrimitiveArrayTag<R>) this;
    }

    public int size() {
        return this.delegate.size();
    }

    public void clear() {
        this.delegate.clear();
    }

    public boolean isEmpty() {
        return this.delegate.isEmpty();
    }

    public TagType getTypeOf() {
        return this.typeOf;
    }

    public IN_F remove(int index) {
        return from(this.delegate.remove(index));
    }

    public boolean remove(IN_F value) {
        return this.delegate.remove(to(value));
    }

    public boolean contains(IN_F value) {
        return this.delegate.contains(to(value));
    }

    public IN_F get(int index) {
        return from(this.delegate.get(index));
    }

    public IN_F set(int index, IN_F value) {
        return from(this.delegate.set(index, to(value)));
    }

    public boolean add(IN_F value) {
        return this.delegate.add(to(value));
    }

    public void addAll(Iterable<IN_F> values) {
        for (IN_F val : values) {
            add(val);
        }
    }

    public Stream<IN_F> stream() {
        return this.delegate.stream().map(this::from);
    }

    public List<IN_F> get() {
        return stream().collect(Collectors.toList());
    }

    public List<IN> getTags() {
        return this.delegate;
    }

    @NotNull
    @Override
    public Iterator<IN_F> iterator() {
        return stream().iterator();
    }
}
