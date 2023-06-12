package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutNBTQuery extends WrappedPacket<WrappedPlayOutNBTQuery> {

    public WrappedPlayOutNBTQuery(Object rawPacket, PacketContainer<WrappedPlayOutNBTQuery> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
