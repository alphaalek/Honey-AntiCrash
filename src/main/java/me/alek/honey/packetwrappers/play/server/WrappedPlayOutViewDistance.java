package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutViewDistance extends WrappedPacket<WrappedPlayOutViewDistance> {

    public WrappedPlayOutViewDistance(Object rawPacket, PacketContainer<WrappedPlayOutViewDistance> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
