package me.alek.honey.netty.wrappers.handshake.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedHandshakeInSetProtocol extends WrappedPacket<WrappedHandshakeInSetProtocol> {

    public WrappedHandshakeInSetProtocol(Object rawPacket, PacketContainer<WrappedHandshakeInSetProtocol> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
