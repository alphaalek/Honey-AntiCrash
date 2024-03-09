package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutWorldEvent extends WrappedPacket<WrappedPlayOutWorldEvent> {

    public WrappedPlayOutWorldEvent(Object rawPacket, PacketContainer<WrappedPlayOutWorldEvent> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
