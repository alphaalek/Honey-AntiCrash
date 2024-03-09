package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutWorldBorder extends WrappedPacket<WrappedPlayOutWorldBorder> {

    public WrappedPlayOutWorldBorder(Object rawPacket, PacketContainer<WrappedPlayOutWorldBorder> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
