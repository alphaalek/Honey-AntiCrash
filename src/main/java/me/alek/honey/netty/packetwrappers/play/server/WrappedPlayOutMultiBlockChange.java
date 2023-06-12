package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutMultiBlockChange extends WrappedPacket<WrappedPlayOutMultiBlockChange> {

    public WrappedPlayOutMultiBlockChange(Object rawPacket, PacketContainer<WrappedPlayOutMultiBlockChange> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
