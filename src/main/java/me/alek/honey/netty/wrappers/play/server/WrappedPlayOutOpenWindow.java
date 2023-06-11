package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutOpenWindow extends WrappedPacket<WrappedPlayOutOpenWindow> {

    public WrappedPlayOutOpenWindow(Object rawPacket, PacketContainer<WrappedPlayOutOpenWindow> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
