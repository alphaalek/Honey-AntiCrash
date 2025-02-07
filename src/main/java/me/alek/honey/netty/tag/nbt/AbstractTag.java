package me.alek.honey.netty.tag.nbt;

import me.alek.honey.netty.modelwrappers.WrappedObject;
import me.alek.honey.netty.structure.IStructureModifier;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.converters.ApplicableConverter;
import me.alek.honey.netty.tag.nbt.array.AbstractArrayTag;
import me.alek.honey.netty.tag.nbt.visitor.TagVisitor;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public abstract class AbstractTag<T, V, TAG extends AbstractTag<T, V, TAG>> extends WrappedObject<AbstractTag<T, V, TAG>> implements Cloneable, Serializable {

    protected final Object handle;
    protected final IStructureModifier<V> valueStructureModifier;

    private TagType type;
    private V value;

    public AbstractTag(Object handle) {
        super(handle);
        this.handle = handle;

        this.valueStructureModifier = applyStructure(getNBTConverter())
                .apply(getReflectionStructure())
                .withTarget(handle);

        getValue();
    }

    @SuppressWarnings("unchecked")
    public TAG incrementBytes(TagLimiter limiter, int bytes) {
        limiter.incrementBytes(bytes);
        return (TAG) this;
    }

    @SuppressWarnings("unchecked")
    public TAG incrementComplexity(TagLimiter limiter) {
        limiter.incrementComplexity();
        return (TAG) this;
    }

    @SuppressWarnings("unchecked")
    public TAG increment(TagLimiter limiter) {
        limiter.incrementBase();
        return (TAG) this;
    }

    public abstract ApplicableConverter<T, V> getNBTConverter();

    public abstract Function<ReflectStructure<Object, ?>, ReflectStructure<T, V>> applyStructure(ApplicableConverter<T, V> converter);

    @SuppressWarnings("unchecked")
    public void write(DataOutputStream output, TagVisitor.Out visitor) throws IOException {
        getType().<TAG>getCodec().write(output, (TAG) this, visitor);
    }

    public V getValue() {
        if (this.value == null) {
            this.value = this.valueStructureModifier.readFieldAndApplyMapping(0);
        }
        return this.value;
    }

    public void setValue(V value) {
        this.value = value;
        this.valueStructureModifier.writeField(0, value);
    }

    public boolean isNull() {
        return this.handle == null;
    }

    public AbstractArrayTag<?, ?, ?> asArray() {
        if (getType().getDataTypeClass() != List.class) {
            return null;
        }
        return (AbstractArrayTag<?, ?, ?>) this;
    }

    public CompoundTag asCompound() {
        if (getType().getCollection() != TagTypeCollection.COMPOUND) {
            return null;
        }
        return (CompoundTag) this;
    }

    @SuppressWarnings("unchecked")
    public <C> PrimitiveTag<C> asPrimitive() {
        if (getType().hasCollection()) {
            return null;
        }
        return (PrimitiveTag<C>) this;
    }

    public TagType getType() {
        if (this.type == null) {
            this.type = NBTFactory.getType(this.handle, false);
        }
        return type;
    }

    private boolean isType(TagType type) {
        return getType().equals(type);
    }

    public boolean isEnd() {
        return isType(TagType.END);
    }

    public boolean isByte() {
        return isType(TagType.BYTE);
    }

    public boolean isShort() {
        return isType(TagType.SHORT);
    }

    public boolean isInt() {
        return isType(TagType.INT);
    }

    public boolean isLong() {
        return isType(TagType.LONG);
    }

    public boolean isFloat() {
        return isType(TagType.FLOAT);
    }

    public boolean isDouble() {
        return isType(TagType.DOUBLE);
    }

    public boolean isByteArray() {
        return isType(TagType.ARRAY_OF_BYTE);
    }

    public boolean isString() {
        return isType(TagType.STRING);
    }

    public boolean isArray() {
        return getType().getCollection() == TagTypeCollection.ARRAY;
    }

    public boolean isCompound() {
        return getType().getCollection() == TagTypeCollection.COMPOUND;
    }

    public boolean isIntArray() {
        return isType(TagType.ARRAY_OF_INT);
    }

    public boolean isLongArray() {
        return isType(TagType.ARRAY_OF_LONG);
    }

    @Override
    public String toString() {
        return handle.toString();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof AbstractTag)) {
            return false;
        }
        AbstractTag<?, ?, ?> base = (AbstractTag<?, ?, ?>) object;
        if (!base.getType().equals(getType())) {
            return false;
        }
        return Objects.equals(getValue(), base.getValue());
    }

    @Override
    public TAG clone() {
        return NBTFactory.copyBase((TAG) this);
    }
}
