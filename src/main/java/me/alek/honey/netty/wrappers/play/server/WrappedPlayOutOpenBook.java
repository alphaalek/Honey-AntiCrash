package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutOpenBook extends WrappedPacket<WrappedPlayOutOpenBook> {

    public WrappedPlayOutOpenBook(Object rawPacket, PacketContainer<WrappedPlayOutOpenBook> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
