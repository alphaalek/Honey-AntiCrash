package me.alek.honey.packetwrappers.status.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedStatusOutServerInfo extends WrappedPacket<WrappedStatusOutServerInfo> {

    public WrappedStatusOutServerInfo(Object rawPacket, PacketContainer<WrappedStatusOutServerInfo> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
