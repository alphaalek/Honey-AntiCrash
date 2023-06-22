package me.alek.honey.netty.modelwrappers.nbt;

import me.alek.honey.netty.api.packet.IStructureModifier;
import me.alek.honey.netty.modelwrappers.WrappedObject;
import me.alek.honey.netty.packet.structure.ReflectStructure;
import me.alek.honey.netty.packet.structure.ReflectStructureCache;

import java.io.Serializable;
import java.util.Set;
import java.util.function.Function;

public abstract class NBTBase<T, V> extends WrappedObject<NBTBase<T, V>> implements Cloneable, Serializable {

    private final Object handle;
    private final ReflectStructure<Object, ?> defaultStructure;
    private final IStructureModifier<?> defaultStructureModifier;
    protected IStructureModifier<V> specificStructureModifier;

    private V value;
    private NBTType type;

    public NBTBase(Object base) {
        this.handle = base;

        defaultStructure = ReflectStructureCache.acquireStructure(handle.getClass());

        defaultStructureModifier = defaultStructure
                .withTarget(base);
        specificStructureModifier = getValueStructure()
                .apply(defaultStructure)
                .withTarget(base);
    }

    public abstract Function<ReflectStructure<Object, ?>, ReflectStructure<T, V>> getValueStructure();

    public Object getHandle() {
        return handle;
    }

    public void setValue(V value) {
        this.value = value;
        specificStructureModifier.writeField(0, value);
    }


    public V getValue() {
        if (value == null) {
            value = specificStructureModifier.readField(0);
        }
        return value;
    }

    public NBTType getType() {
        if (type == null) {
            this.type = NBTFactory.getType(handle);
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
        return base.getType() == getType();
    }

    @Override
    public NBTBase<T, V> clone() {
        return NBTFactory.copyBase(this);
    }
}
