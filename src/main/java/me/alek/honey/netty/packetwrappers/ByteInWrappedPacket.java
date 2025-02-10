package me.alek.honey.netty.packetwrappers;

import me.alek.honey.netty.codec.PacketDataSerializer;

public interface ByteInWrappedPacket<WP extends SourcedWrappedPacket<WP>> extends SourcedWrappedPacket<WP>, ByteWrappedPacket<WP> {

    PacketDataSerializer getBuf();

    int getByteSize();

    int getAccumulatedByteSize();

    int getDoubleByteSequences();
}
