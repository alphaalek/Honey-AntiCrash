package me.alek.honey.netty.packetwrappers.handshake.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedHandshakeInSetProtocol extends WrappedPacket<WrappedHandshakeInSetProtocol> {

    public WrappedHandshakeInSetProtocol(Object rawPacket, PacketContainer<WrappedHandshakeInSetProtocol> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
