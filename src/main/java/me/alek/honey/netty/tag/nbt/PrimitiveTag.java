package me.alek.honey.netty.tag.nbt;

import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.converters.ApplicableConverter;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.function.Function;

public class PrimitiveTag<T> extends AbstractTag<T, T, PrimitiveTag<T>> {

    public static final PrimitiveTag<Void> END = new PrimitiveTag<>(TagType.END);

    public PrimitiveTag(Object base) {
        super(base);
    }

    public PrimitiveTag(TagType type) {
        super(type.createRawDelegate());
    }

    public PrimitiveTag(TagType type, T value) {
        super(type.getBaseInvoker().invoke(value));
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
    public void write(DataOutputStream output) throws IOException {
        getType().<PrimitiveTag<T>>getCodec().write(output, this);
    }
}
