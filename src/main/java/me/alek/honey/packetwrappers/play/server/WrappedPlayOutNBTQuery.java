package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutNBTQuery extends WrappedPacket<WrappedPlayOutNBTQuery> {

    public WrappedPlayOutNBTQuery(Object rawPacket, PacketContainer<WrappedPlayOutNBTQuery> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
