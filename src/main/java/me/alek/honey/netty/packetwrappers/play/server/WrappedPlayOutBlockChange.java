package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutBlockChange extends WrappedPacket<WrappedPlayOutBlockChange> {

    public WrappedPlayOutBlockChange(Object rawPacket, PacketContainer<WrappedPlayOutBlockChange> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
