package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutBlockChange extends WrappedPacket<WrappedPlayOutBlockChange> {

    public WrappedPlayOutBlockChange(Object rawPacket, PacketContainer<WrappedPlayOutBlockChange> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
