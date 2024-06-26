package me.alek.honey.packetwrappers.handshake.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.PacketReader;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.util.protocol.ClientInfo;

public class WrappedHandshakeInSetProtocol extends WrappedPacket<WrappedHandshakeInSetProtocol> {

    public WrappedHandshakeInSetProtocol(Object rawPacket, PacketContainer<WrappedHandshakeInSetProtocol> packetContainer) {
        super(rawPacket, packetContainer);
    }

    public ClientInfo getClientInfo() {
        return new ClientInfo(getContainer().getInts().readField(0), getContainer().getStrings().readField(0));
    }

    public static class Reader implements PacketReader.Buf<PacketDataSerializer> {

        @Override
        public Object[] read(PacketDataSerializer val) {
            return new Object[]{val.readVarInt(), val.readString(Short.MAX_VALUE), val.readUnsignedShort(), val.readVarInt()};
        }
    }
}
