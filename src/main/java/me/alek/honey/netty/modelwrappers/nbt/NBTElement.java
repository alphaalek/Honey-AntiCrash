package me.alek.honey.netty.modelwrappers.nbt;

import me.alek.honey.netty.packet.structure.ReflectStructure;

import java.util.function.Function;

public class NBTElement<T> extends NBTBase<T, T> {

    public NBTElement(Object base) {
        super(base);
    }

    @Override
    public Function<ReflectStructure<Object, ?>, ReflectStructure<T, T>> getValueStructure() {
        return (structure) -> structure.withType((Class<T>) getType().getClazz());
    }
}
