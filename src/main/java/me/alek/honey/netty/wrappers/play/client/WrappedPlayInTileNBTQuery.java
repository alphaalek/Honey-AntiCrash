package me.alek.honey.netty.wrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayInTileNBTQuery extends WrappedPacket<WrappedPlayInTileNBTQuery> {

    public WrappedPlayInTileNBTQuery(Object rawPacket, PacketContainer<WrappedPlayInTileNBTQuery> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
