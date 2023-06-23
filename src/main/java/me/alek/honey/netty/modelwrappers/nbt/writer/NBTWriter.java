package me.alek.honey.netty.modelwrappers.nbt.writer;

import me.alek.honey.netty.modelwrappers.nbt.NBTElement;

import java.io.DataOutput;

public interface NBTWriter<T> {

    void write(DataOutput output, NBTElement<T> element) throws Exception;
}
