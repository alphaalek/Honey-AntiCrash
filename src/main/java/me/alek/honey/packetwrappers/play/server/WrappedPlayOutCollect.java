package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutCollect extends WrappedPacket<WrappedPlayOutCollect> {

    public WrappedPlayOutCollect(Object rawPacket, PacketContainer<WrappedPlayOutCollect> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
