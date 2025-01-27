package me.alek.honey.netty.tag.nbt.array;

import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.converters.ApplicableConverter;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.structure.converters.ListConverter;
import me.alek.honey.netty.tag.nbt.AbstractTag;
import me.alek.honey.netty.tag.nbt.NBTFactory;
import me.alek.honey.netty.tag.nbt.TagType;
import org.bukkit.Bukkit;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public abstract class AbstractArrayTag<IN extends AbstractTag<?, ?, IN>, ARRAY extends AbstractArrayTag<IN, ARRAY, C>, C> extends AbstractTag<List<Object>, ListConverter<Object, IN>, ARRAY> implements Iterable<C> {

    private static final TagType TYPE = TagType.getFromCollection(TagType.Collection.ARRAY);

    private ListConverter<Object, IN> delegate;
    protected TagType typeOf;

    public AbstractArrayTag(Object base, TagType typeOf) {
        super(base);
        this.typeOf = typeOf;
    }

    public AbstractArrayTag(TagType typeOf) {
        this(typeOf, new ArrayList<>());
    }

    public AbstractArrayTag(TagType typeOf, Iterable<C> array) {
        super(createBaseFromArray(typeOf));
        this.typeOf = typeOf;

        for (C val : array) {
            getValue().addConverted(to(val));
        }
        if (this.typeOf != null) {
            ReflectStructure.Provider.acquireStructure(TYPE.getDelegateClass())
                    .withBytes().withTarget(getHandle()).writeField(0, this.typeOf.getTypeId());
        }
    }

    private static Object createBaseFromArray(TagType typeOf) {
        if (TYPE.equals(TagType.ARRAY_LEGACY)) {
            return TYPE.createRawDelegate();
        } else if (TYPE.equals(TagType.ARRAY_MODERN)) {
            return TYPE.getNMSInvoker().invoke(new ArrayList<>(), Optional.ofNullable(typeOf).map(TagType::getTypeId).orElse((byte) 0));
        } else {
            throw new IllegalStateException();
        }
    }

    public C from(IN in) {
        // to be overridden
        return (C) in;
    }

    public IN to(C context) {
        // to be overridden
        return (IN) context;
    }

    @Override
    public void write(DataOutputStream output) throws IOException {
        getType().<ARRAY>getCodec().write(output, (ARRAY) this);
    }

    public Stream<C> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    @Override
    public ApplicableConverter<List<Object>, ListConverter<Object, IN>> getNBTConverter() {
        if (this.delegate == null) {
            this.delegate = Converters.getNBTListConverter();
        }
        return this.delegate;
    }

    @Override
    public Function<ReflectStructure<Object, ?>, ReflectStructure<List<Object>, ListConverter<Object, IN>>> applyStructure(ApplicableConverter<List<Object>, ListConverter<Object, IN>> converter) {
        return (structure) -> structure.withSpecificType(List.class, converter, false);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ArrayTag<IN> asArray() {
        if (getTypeOf().getCollection() == TagType.Collection.ARRAY
                || getTypeOf().getCollection() == TagType.Collection.COMPOUND) {
            return null;
        }

        return (ArrayTag<IN>) this;
    }

    @SuppressWarnings("unchecked")
    public <R> ArrayTag<R> asArrayOf(Class<R> clazz) {
        if (getTypeOf().getCollection() == TagType.Collection.ARRAY
                || getTypeOf().getCollection() == TagType.Collection.COMPOUND) {
            return null;
        }

        return (ArrayTag<R>) this;
    }


    @SuppressWarnings("unchecked")
    public <ARRAY2 extends GeneticArrayTypes.ArrayOfArrayTag<ARRAY2, IN2>, IN2 extends AbstractArrayTag<?, IN2, ?>> GeneticArrayTypes.ArrayOfArrayTag<ARRAY2, IN2> asArrayOfArray() {
        if (getTypeOf().getCollection() != TagType.Collection.ARRAY) {
            return null;
        }
        return (GeneticArrayTypes.ArrayOfArrayTag<ARRAY2, IN2>) this;
    }

    public GeneticArrayTypes.ArrayOfCompoundTag asArrayOfCompound() {
        if (getTypeOf().getCollection() != TagType.Collection.COMPOUND) {
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
        if (this.typeOf == null) {
            this.typeOf = NBTFactory.getArrayType(this.handle);
        }
        return this.typeOf;
    }

    public C remove(int index) {
        return from(getValue().removeConverted(index));
    }

    public boolean remove(C value) {
        return getValue().removeConverted(to(value));
    }

    public boolean contains(C value) {
        return getValue().containsConverted(to(value));
    }

    public C get(int index) {
        return from(getValue().getConverted(index));
    }

    public C set(int index, C value) {
        return from(getValue().setConverted(index, to(value)));
    }

    public boolean add(C value) {
        return getValue().addConverted(to(value));
    }

    public void addAll(Iterable<C> values) {
        for (C val : values) {
            add(val);
        }
    }

    public List<IN> getValues() {
        return getValue().getDelegatedList();
    }
}
