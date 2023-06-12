package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutOpenBook extends WrappedPacket<WrappedPlayOutOpenBook> {

    public WrappedPlayOutOpenBook(Object rawPacket, PacketContainer<WrappedPlayOutOpenBook> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
