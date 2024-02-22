package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayInEntityNBTQuery extends WrappedPacket<WrappedPlayInEntityNBTQuery> {

    public WrappedPlayInEntityNBTQuery(Object rawPacket, PacketContainer<WrappedPlayInEntityNBTQuery> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
