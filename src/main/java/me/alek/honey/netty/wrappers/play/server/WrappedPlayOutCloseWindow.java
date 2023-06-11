package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutCloseWindow extends WrappedPacket<WrappedPlayOutCloseWindow> {

    public WrappedPlayOutCloseWindow(Object rawPacket, PacketContainer<WrappedPlayOutCloseWindow> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
