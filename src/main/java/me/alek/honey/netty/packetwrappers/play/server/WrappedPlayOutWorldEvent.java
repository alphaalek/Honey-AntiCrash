package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutWorldEvent extends WrappedPacket<WrappedPlayOutWorldEvent> {

    public WrappedPlayOutWorldEvent(Object rawPacket, PacketContainer<WrappedPlayOutWorldEvent> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
