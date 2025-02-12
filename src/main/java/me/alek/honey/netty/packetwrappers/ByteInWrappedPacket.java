package me.alek.honey.netty.packetwrappers;

import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.util.protocol.NetProtocol;

public interface ByteInWrappedPacket<WP extends SourcedWrappedPacket<WP>> extends SourcedWrappedPacket<WP>, ByteWrappedPacket<WP> {

    default boolean shouldRecallInitialByte(NetProtocol newProtocol) {
        return false;
    }

    PacketDataSerializer serializer();

    int bytes();

    int accumulatedBytes();

    int doubleByteSequences();
}
