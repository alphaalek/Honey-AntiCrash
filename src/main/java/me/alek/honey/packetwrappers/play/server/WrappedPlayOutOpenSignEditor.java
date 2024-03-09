package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutOpenSignEditor extends WrappedPacket<WrappedPlayOutOpenSignEditor> {

    public WrappedPlayOutOpenSignEditor(Object rawPacket, PacketContainer<WrappedPlayOutOpenSignEditor> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
