package me.alek.honey.netty.packetwrappers.status.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedStatusInStart extends WrappedPacket<WrappedStatusInStart> {

    public WrappedStatusInStart(Object rawPacket, PacketContainer<WrappedStatusInStart> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
