package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutMap extends WrappedPacket<WrappedPlayOutMap> {

    public WrappedPlayOutMap(Object rawPacket, PacketContainer<WrappedPlayOutMap> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
