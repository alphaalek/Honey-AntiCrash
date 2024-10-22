package me.alek.honey.netty.nbt.writer;

import me.alek.honey.netty.nbt.NBTElement;

import java.io.DataOutput;

public interface NBTWriter<T> {

    void write(DataOutput output, NBTElement<T> element) throws Exception;
}
