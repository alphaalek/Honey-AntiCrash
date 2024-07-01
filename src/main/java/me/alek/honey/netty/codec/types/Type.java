package me.alek.honey.netty.codec.types;

import io.netty.buffer.ByteBuf;

public interface Type<T, B extends ByteBuf> {

    T read(B buf, Object... params);

    B write(B buf, T val, Object... params);
}
