package me.alek.honey.packetwrappers.status.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedStatusInStart extends WrappedPacket<WrappedStatusInStart> {

    public WrappedStatusInStart(Object rawPacket, PacketContainer<WrappedStatusInStart> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
