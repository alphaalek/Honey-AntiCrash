package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutWindowData extends WrappedPacket<WrappedPlayOutWindowData> {

    public WrappedPlayOutWindowData(Object rawPacket, PacketContainer<WrappedPlayOutWindowData> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
