package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutUpdateAttributes extends WrappedPacket<WrappedPlayOutUpdateAttributes> {

    public WrappedPlayOutUpdateAttributes(Object rawPacket, PacketContainer<WrappedPlayOutUpdateAttributes> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
