package me.alek.honey.packetwrappers.status.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedStatusOutPong extends WrappedPacket<WrappedStatusOutPong> {

    public WrappedStatusOutPong(Object rawPacket, PacketContainer<WrappedStatusOutPong> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
