package me.alek.honey.netty.tag.nbt;

import me.alek.honey.netty.tag.nbt.visitor.NBTVisitor;

import java.io.DataOutputStream;
import java.io.IOException;

public abstract class AbstractTag<T, TAG extends AbstractTag<T, TAG>> implements Cloneable {

    protected final TagType type;

    public AbstractTag(TagType type) {
        this.type = type;
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

    @SuppressWarnings("unchecked")
    public void write(DataOutputStream output, NBTVisitor.Out visitor) throws IOException {
        getType().<TAG>getCodec().write(output, (TAG) this, visitor);
    }

    @SuppressWarnings("unchecked")
    public <ARRAY extends ArrayTag<?, ?, ARRAY>> ARRAY asArray() {
        return (ARRAY) this;
    }

    @SuppressWarnings("unchecked")
    public <C> PrimitiveTag<C> asPrimitive() {
        return (PrimitiveTag<C>) this;
    }

    public CompoundTag asCompound() {
        return (CompoundTag) this;
    }

    public TagType getType() {
        return this.type;
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

    public boolean isString() {
        return isType(TagType.STRING);
    }

    public boolean isArray() {
        return getType() == PrimitiveArrayTag.TYPE.get();
    }

    public boolean isCompound() {
        return getType() == CompoundTag.TYPE.get();
    }

    public boolean isIntArray() {
        return isType(TagType.ARRAY_OF_INT);
    }

    public boolean isLongArray() {
        return isType(TagType.ARRAY_OF_LONG);
    }

    public boolean isByteArray() {
        return isType(TagType.ARRAY_OF_BYTE);
    }

    public abstract String toString();

    @SuppressWarnings("unchecked")
    @Override
    public TAG clone() {
        try {
            return (TAG) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
