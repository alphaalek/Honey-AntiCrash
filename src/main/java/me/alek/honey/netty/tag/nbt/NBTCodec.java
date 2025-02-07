package me.alek.honey.netty.tag.nbt;

import me.alek.honey.netty.tag.nbt.visitor.TagVisitor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface NBTCodec<T extends AbstractTag<?, ?, T>> {

    void write(DataOutputStream output, T tag, TagVisitor.Out visitor) throws IOException;

    T read(DataInputStream input, TagLimiter limiter, TagVisitor.In visitor) throws IOException;

    interface Primitive<T> extends NBTCodec<PrimitiveTag<T>> {

    }
}
