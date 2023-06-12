package me.alek.honey.netty.packetwrappers.status.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedStatusInPing extends WrappedPacket<WrappedStatusInPing> {

    public WrappedStatusInPing(Object rawPacket, PacketContainer<WrappedStatusInPing> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
