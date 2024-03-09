package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutWindowItems extends WrappedPacket<WrappedPlayOutWindowItems> {

    public WrappedPlayOutWindowItems(Object rawPacket, PacketContainer<WrappedPlayOutWindowItems> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
