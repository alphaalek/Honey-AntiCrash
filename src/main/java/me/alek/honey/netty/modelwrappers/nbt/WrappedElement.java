package me.alek.honey.netty.modelwrappers.nbt;

import me.alek.honey.netty.packet.structure.ReflectStructure;

import java.util.function.Function;

public class WrappedElement<T> extends NBTBase<T, T> {

    public WrappedElement(Object base) {
        super(base);
    }

    @Override
    public Function<ReflectStructure<Object, ?>, ReflectStructure<T, T>> getStructure() {
        return null;
    }
}
