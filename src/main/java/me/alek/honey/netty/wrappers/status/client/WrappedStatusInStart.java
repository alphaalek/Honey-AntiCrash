package me.alek.honey.netty.wrappers.status.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedStatusInStart extends WrappedPacket<WrappedStatusInStart> {

    public WrappedStatusInStart(Object rawPacket, PacketContainer<WrappedStatusInStart> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
