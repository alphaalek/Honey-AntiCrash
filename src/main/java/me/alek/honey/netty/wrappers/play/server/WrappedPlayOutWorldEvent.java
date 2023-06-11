package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutWorldEvent extends WrappedPacket<WrappedPlayOutWorldEvent> {

    public WrappedPlayOutWorldEvent(Object rawPacket, PacketContainer<WrappedPlayOutWorldEvent> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
