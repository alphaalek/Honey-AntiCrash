package me.alek.honey.netty.packetwrappers.handshake.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.PacketReader;
import me.alek.honey.netty.packetwrappers.ObjectWrappedPacket;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.processor.Processor;
import me.alek.honey.util.ClientInfo;

public class WrappedHandshakeInSetProtocol extends ObjectWrappedPacket<WrappedHandshakeInSetProtocol> {

    public WrappedHandshakeInSetProtocol(Object rawPacket, PacketContainer<WrappedHandshakeInSetProtocol> packetContainer) {
        super(rawPacket, packetContainer);
    }

    public static class Reader implements PacketReader.Buf<PacketDataSerializer> {

        @Override
        public Object[] read(PacketDataSerializer val) {
            return new Object[]{val.readVarInt(), val.readString(Short.MAX_VALUE), val.readUnsignedShort(), val.readVarInt()};
        }
    }
}
