package me.alek.honey.netty.nbt;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface NBTCodec<T extends AbstractTag<?, ?, T>> {

    void write(DataOutputStream output, T tag) throws IOException;

    T read(DataInputStream input, TagLimiter limiter) throws IOException;

    interface Element<T> extends NBTCodec<PrimitiveTag<T>> {

    }
}
