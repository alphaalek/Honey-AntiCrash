package me.alek.honey.netty.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public interface NBTCodec<T extends NBTBase<?, ?>> {

    void write(DataOutput output, T tag) throws IOException;

    T read(DataInput input) throws IOException;

    interface Element<T> extends NBTCodec<NBTElement<T>> {

    }
}
