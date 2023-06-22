package me.alek.honey.netty.modelwrappers.nbt;

import me.alek.honey.netty.api.packet.IStructureModifier;
import me.alek.honey.netty.modelwrappers.WrappedObject;
import me.alek.honey.netty.packet.structure.ReflectStructure;
import me.alek.honey.netty.packet.structure.ReflectStructureCache;

import java.util.function.Function;

public abstract class NBTBase<T, V> extends WrappedObject<NBTBase<T, V>> {

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
        specificStructureModifier = getStructure()
                .apply(defaultStructure)
                .withTarget(base);
    }

    public abstract Function<ReflectStructure<Object, ?>, ReflectStructure<T, V>> getStructure();

    public Object getHandle() {
        return handle;
    }

    public void setValue(V value) {
        this.value = value;

        if (specificStructureModifier != null) {
            specificStructureModifier.writeField(0, value);
        }
    }

    public V getValue() {
        if (value == null && specificStructureModifier != null) {
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

}
