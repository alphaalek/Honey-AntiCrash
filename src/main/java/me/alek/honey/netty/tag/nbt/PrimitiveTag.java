package me.alek.honey.netty.tag.nbt;

import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.converters.ApplicableConverter;
import me.alek.honey.util.protocol.NetProtocol;

import java.util.function.Function;

public class PrimitiveTag<T> extends AbstractTag<T, T, PrimitiveTag<T>> {

    public static final PrimitiveTag<Void> END = new PrimitiveTag<>(TagType.END);

    public PrimitiveTag(Object base) {
        super(base);
    }

    public PrimitiveTag(TagType type) {
        super(createRaw(type));
    }

    private static Object createRaw(TagType type) {
        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_15) && !type.equals(TagType.END)) {
            throw new IllegalStateException("Can only create raw types up to 1.14.4");
        }
        return type.getNMSInvoker().invoke();
    }

    public PrimitiveTag(TagType type, T value) {
        super(type.getNMSInvoker().invoke(value));
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
}
