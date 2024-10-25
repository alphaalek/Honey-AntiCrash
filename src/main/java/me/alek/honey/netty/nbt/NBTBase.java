package me.alek.honey.netty.nbt;

import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.modelwrappers.WrappedObject;
import me.alek.honey.netty.nbt.array.NBTAbstractArray;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.converters.ApplicableConverter;

import java.io.DataOutput;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public abstract class NBTBase<T, V> extends WrappedObject<NBTBase<T, V>> implements Cloneable, Serializable {

    private final Object handle;
    protected final boolean forceCreateChildren;
    protected final IStructureModifier<V> valueStructureModifier;

    private NBTRoot root;
    private NBTBase<?, ?> owner;
    private NBTType type;
    private V value;

    public NBTBase(Object handle, NBTRoot root, NBTBase<?, ?> owner, boolean forceCreation) {
        super(handle);
        this.handle = handle;
        this.root = root;
        this.owner = owner;
        this.forceCreateChildren = forceCreation;

        if (handle != null) {
            valueStructureModifier = applyStructure(getNBTConverter())
                    .apply(getReflectionStructure())
                    .withTarget(handle);
        } else {
            valueStructureModifier = null;
        }

        if (getRoot().getTotalElements() > getRoot().getMaximumElementsWithin()) {
            return;
        }

        if (handle != null) {
            getRoot().subscribe(getOwner(), this);
        }

        if (forceCreation) {
            getValue();
        }
    }

    public NBTBase(Object base) {
        this(base, new NBTRoot(Integer.MAX_VALUE), null, true);
    }

    public abstract ApplicableConverter<T, V> getNBTConverter();

    public abstract Function<ReflectStructure<Object, ?>, ReflectStructure<T, V>> applyStructure(ApplicableConverter<T, V> converter);

    public abstract void write(DataOutput output) throws Exception;

    public V getValue() {
        if (value == null) {
            value = valueStructureModifier.readFieldAndApplyMapping(0);
        }
        return value;
    }

    public void setValue(V value) {
        this.value = value;
        valueStructureModifier.writeField(0, value);
    }

    public boolean isNull() {
        return valueStructureModifier == null || getReflectionStructure() == null || getHandle() == null;
    }

    public NBTRoot getRoot() {
        return root;
    }

    public void setRoot(NBTRoot root) {
        if (this.root != null) {
            for (NBTType nbtType : NBTType.values()) {
                for (NBTBase<?, ?> nbtBase : new ArrayList<NBTBase<?, ?>>(root.getAllElementsAs(nbtType))) {
                    this.root.unsubscribe(nbtBase);
                }
            }
        }

        this.root = root;
        if (this.root != null) {
            for (NBTType nbtType : NBTType.values()) {
                for (NBTBase<?, ?> nbtBase : new ArrayList<NBTBase<?, ?>>(this.root.getAllElementsAs(nbtType))) {
                    this.root.subscribe(nbtBase.getOwner(), nbtBase);
                }
            }
        }
    }

    public NBTBase<?, ?> getOwner() {
        if (owner == null) {
            owner = this;
        }
        return owner;
    }

    public void setOwner(NBTBase<?, ?> owner) {
        this.owner = owner;
    }

    public NBTAbstractArray<?> asList() {
        if (type.getClazz() != List.class) {
            return null;
        }
        return (NBTAbstractArray<?>) this;
    }

    public NBTCompound asCompound() {
        if (type != NBTType.COMPOUND) {
            return null;
        }
        return (NBTCompound) this;
    }

    public NBTElement<V> asElement() {
        if (type.getClazz() == List.class || type.getClazz() == Map.class) {
            return null;
        }
        return (NBTElement<V>) this;
    }

    public <R> NBTElement<R> asElement(Class<R> clazz) {
        if (type.getClazz() == List.class || type.getClazz() == Map.class) {
            return null;
        }
        return (NBTElement<R>) this;
    }

    public NBTType getType() {
        if (type == null) {
            this.type = NBTFactory.getType(handle, false, getRoot(), getOwner(), forceCreateChildren);
        }
        return type;
    }

    public boolean equals(NBTBase<?, ?> nbtBase) {
        return getType() == nbtBase.getType();
    }

    private boolean isType(NBTType type) {
        return getType().equals(type);
    }

    public boolean isEnd() {
        return isType(NBTType.END);
    }

    public boolean isByte() {
        return isType(NBTType.BYTE);
    }

    public boolean isShort() {
        return isType(NBTType.SHORT);
    }

    public boolean isInt() {
        return isType(NBTType.INT);
    }

    public boolean isLong() {
        return isType(NBTType.LONG);
    }

    public boolean isFloat() {
        return isType(NBTType.FLOAT);
    }

    public boolean isDouble() {
        return isType(NBTType.DOUBLE);
    }

    public boolean isByteArray() {
        return isType(NBTType.ARRAY_OF_BYTE);
    }

    public boolean isString() {
        return isType(NBTType.STRING);
    }

    public boolean isList() {
        return isType(NBTType.ARRAY);
    }

    public boolean isCompound() {
        return isType(NBTType.COMPOUND);
    }

    public boolean isIntArray() {
        return isType(NBTType.ARRAY_OF_INT);
    }

    public boolean isLongArray() {
        return isType(NBTType.ARRAY_OF_LONG);
    }

    @Override
    public String toString() {
        return handle.toString();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof NBTBase)) {
            return false;
        }
        NBTBase<?, ?> base = (NBTBase<?, ?>) object;
        if (base.getType() != getType()) {
            return false;
        }
        return this == object;
    }

    @Override
    public NBTBase<T, V> clone() {
        return NBTFactory.copyBase(this, getRoot(), getOwner());
    }
}
