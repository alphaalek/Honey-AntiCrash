package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutOpenWindowHorse extends WrappedPacket<WrappedPlayOutOpenWindowHorse> {

    public WrappedPlayOutOpenWindowHorse(Object rawPacket, PacketContainer<WrappedPlayOutOpenWindowHorse> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
