package me.alek.honey.netty.modelwrappers.nbt;

import me.alek.honey.netty.api.packet.IStructureModifier;
import me.alek.honey.netty.modelwrappers.WrappedObject;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.converters.ApplicableConverter;

import java.io.DataOutput;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public abstract class NBTBase<T, V> extends WrappedObject<NBTBase<T, V>> implements Cloneable, Serializable {

    private Object handle;
    protected IStructureModifier<V> valueStructureModifier;

    private NBTRoot root;
    private NBTBase<?, ?> owner;
    private NBTType type;
    private V value;


    public NBTBase(Object base, NBTRoot root, NBTBase<?, ?> owner) {
        super(base);
        this.handle = base;
        this.root = root;
        this.owner = owner;

        if (getRoot().getTotalElements() <= getRoot().getMaximumElementsWithin()) { // sætter limit på elementer i nbt structure
            if (base != null) {
                valueStructureModifier = applyStructure(getConverter())
                        .apply(getReflectStructure())
                        .withTarget(base);
            }
            getRoot().subscribe(getOwner(), this);
        }
    }


    public abstract ApplicableConverter<T, V> getConverter();

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
        return valueStructureModifier == null || getReflectStructure() == null || getHandle() == null;
    }

    public NBTRoot getRoot() {
        if (root == null) {
            if (this instanceof NBTRoot) {
                root = (NBTRoot) this;
            }
        }
        return root;
    }

    public NBTBase<?, ?> getOwner() {
        if (owner == null) {
            owner = this;
        }
        return owner;
    }

    public NBTAbstractList<?> asList() {
        if (type.getClazz() != List.class) {
            return null;
        }
        return (NBTAbstractList<?>) this;
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
            this.type = NBTFactory.getType(handle, false, root);
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
        return isType(NBTType.BYTEARRAY);
    }

    public boolean isString() {
        return isType(NBTType.STRING);
    }

    public boolean isList() {
        return isType(NBTType.LIST);
    }

    public boolean isCompound() {
        return isType(NBTType.COMPOUND);
    }

    public boolean isIntArray() {
        return isType(NBTType.INTARRAY);
    }

    public boolean isLongArray() {
        return isType(NBTType.LONGARRAY);
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
        return Objects.equals(this, object);
    }

    @Override
    public NBTBase<T, V> clone() {
        return NBTFactory.copyBase(this, getOwner());
    }
}
