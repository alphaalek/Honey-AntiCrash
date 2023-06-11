package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutNBTQuery extends WrappedPacket<WrappedPlayOutNBTQuery> {

    public WrappedPlayOutNBTQuery(Object rawPacket, PacketContainer<WrappedPlayOutNBTQuery> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
