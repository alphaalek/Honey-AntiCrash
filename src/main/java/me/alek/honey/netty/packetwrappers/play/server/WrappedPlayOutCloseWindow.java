package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutCloseWindow extends WrappedPacket<WrappedPlayOutCloseWindow> {

    public WrappedPlayOutCloseWindow(Object rawPacket, PacketContainer<WrappedPlayOutCloseWindow> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
