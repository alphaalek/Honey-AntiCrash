package me.alek.honey.netty.nbt;

import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.converters.ApplicableConverter;

import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class NBTElement<T> extends NBTBase<T, T> {

    public static final NBTElement<Void> END = new NBTElement<>(NBTType.END);

    public NBTElement(Object base, NBTRoot root, NBTBase<?, ?> owner, boolean forceCreation) {
        super(base, root, owner, forceCreation);
    }

    public NBTElement(NBTType type) {
        super(type.createRawDelegate());
    }

    public NBTElement(NBTType type, T value) {
        super(type.getElementBaseInvoker().invoke(value));
    }

    @Override
    public ApplicableConverter<T, T> getNBTConverter() {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Function<ReflectStructure<Object, ?>, ReflectStructure<T, T>> applyStructure(ApplicableConverter<T, T> converter) {
        return (structure) -> structure.withType((Class<T>) getType().getDataTypeClass());
    }

    @Override
    public void write(DataOutput output) throws IOException {
        getType().getCodec().write(output, this);
    }
}
