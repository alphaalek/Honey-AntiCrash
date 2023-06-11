package me.alek.honey.netty.wrappers.status.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedStatusOutPong extends WrappedPacket<WrappedStatusOutPong> {

    public WrappedStatusOutPong(Object rawPacket, PacketContainer<WrappedStatusOutPong> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
