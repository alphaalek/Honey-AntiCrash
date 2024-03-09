package me.alek.honey.packetwrappers.status.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedStatusInPing extends WrappedPacket<WrappedStatusInPing> {

    public WrappedStatusInPing(Object rawPacket, PacketContainer<WrappedStatusInPing> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
