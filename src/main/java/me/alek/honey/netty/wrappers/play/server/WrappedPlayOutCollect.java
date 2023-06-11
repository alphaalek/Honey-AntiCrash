package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutCollect extends WrappedPacket<WrappedPlayOutCollect> {

    public WrappedPlayOutCollect(Object rawPacket, PacketContainer<WrappedPlayOutCollect> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
