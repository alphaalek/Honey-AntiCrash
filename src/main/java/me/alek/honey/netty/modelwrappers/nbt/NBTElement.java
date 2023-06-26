package me.alek.honey.netty.modelwrappers.nbt;

import me.alek.honey.netty.modelwrappers.nbt.writer.NBTWriter;
import me.alek.honey.netty.modelwrappers.nbt.writer.Writers;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.converters.ApplicableConverter;

import java.io.DataOutput;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class NBTElement<T> extends NBTBase<T, T> {

    private final static Map<NBTType, NBTWriter<?>> WRITERS = new ConcurrentHashMap<>();

    public NBTElement(Object base, NBTRoot root, NBTBase<?, ?> owner) {
        super(base, root, owner);
    }

    @Override
    public ApplicableConverter<T, T> getConverter() {
        return null;
    }

    @Override
    public Function<ReflectStructure<Object, ?>, ReflectStructure<T, T>> applyStructure(ApplicableConverter<T, T> converter) {
        return (structure) -> structure.withType((Class<T>) getType().getClazz());
    }

    @Override
    public void write(DataOutput output) throws Exception {
        if (getType() == NBTType.END) {
            return;
        }
        NBTWriter<?> writer = WRITERS.get(getType());
        if (writer == null ){

            switch (getType()) {
                case BYTE:
                    writer = new Writers.ByteWriter();
                    break;
                case SHORT:
                    writer = new Writers.ShortWriter();
                    break;
                case INT:
                    writer = new Writers.IntWriter();
                    break;
                case LONG:
                    writer = new Writers.LongWriter();
                    break;
                case FLOAT:
                    writer = new Writers.FloatWriter();
                    break;
                case DOUBLE:
                    writer = new Writers.DoubleWriter();
                    break;
                case BYTEARRAY:
                    writer = new Writers.ByteArrayWriter();
                    break;
                case STRING:
                    writer = new Writers.StringWriter();
                    break;
                case INTARRAY:
                    writer = new Writers.IntArrayWriter();
                    break;
                case LONGARRAY:
                    writer = new Writers.LongArrayWriter();
                    break;
            }
            if (writer == null) {
                return;
            }
            WRITERS.put(getType(), writer);
            write(output);
        }
        else {
            try {
                ((NBTWriter<T>)writer).write(output, this);
            } catch (ClassCastException ex) {
                ex.printStackTrace();
            }
        }
    }
}
