package me.alek.honey.netty.packet;

import io.netty.buffer.ByteBuf;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public interface PacketReader<T> {

    Object[] read(T val);

    interface Container<WP extends WrappedPacket<WP>> extends PacketReader<PacketContainer<WP>> {

    }

    interface Buf<B extends ByteBuf> extends PacketReader<B> {

    }
}
