package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutOpenSignEditor extends WrappedPacket<WrappedPlayOutOpenSignEditor> {

    public WrappedPlayOutOpenSignEditor(Object rawPacket, PacketContainer<WrappedPlayOutOpenSignEditor> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
