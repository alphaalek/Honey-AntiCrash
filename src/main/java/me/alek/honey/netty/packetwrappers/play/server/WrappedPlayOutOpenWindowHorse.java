package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutOpenWindowHorse extends WrappedPacket<WrappedPlayOutOpenWindowHorse> {

    public WrappedPlayOutOpenWindowHorse(Object rawPacket, PacketContainer<WrappedPlayOutOpenWindowHorse> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
