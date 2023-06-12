package me.alek.honey.netty.packetwrappers.status.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedStatusOutServerInfo extends WrappedPacket<WrappedStatusOutServerInfo> {

    public WrappedStatusOutServerInfo(Object rawPacket, PacketContainer<WrappedStatusOutServerInfo> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
