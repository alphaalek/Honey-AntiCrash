package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutUpdateAttributes extends WrappedPacket<WrappedPlayOutUpdateAttributes> {

    public WrappedPlayOutUpdateAttributes(Object rawPacket, PacketContainer<WrappedPlayOutUpdateAttributes> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
