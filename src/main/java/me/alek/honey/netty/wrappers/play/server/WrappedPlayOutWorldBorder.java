package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutWorldBorder extends WrappedPacket<WrappedPlayOutWorldBorder> {

    public WrappedPlayOutWorldBorder(Object rawPacket, PacketContainer<WrappedPlayOutWorldBorder> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
