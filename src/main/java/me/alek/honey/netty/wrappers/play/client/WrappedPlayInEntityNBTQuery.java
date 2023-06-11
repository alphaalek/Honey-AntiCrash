package me.alek.honey.netty.wrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayInEntityNBTQuery extends WrappedPacket<WrappedPlayInEntityNBTQuery> {

    public WrappedPlayInEntityNBTQuery(Object rawPacket, PacketContainer<WrappedPlayInEntityNBTQuery> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
