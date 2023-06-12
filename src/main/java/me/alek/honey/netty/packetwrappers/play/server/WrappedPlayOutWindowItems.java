package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutWindowItems extends WrappedPacket<WrappedPlayOutWindowItems> {

    public WrappedPlayOutWindowItems(Object rawPacket, PacketContainer<WrappedPlayOutWindowItems> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
