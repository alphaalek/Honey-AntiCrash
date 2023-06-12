package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayInTileNBTQuery extends WrappedPacket<WrappedPlayInTileNBTQuery> {

    public WrappedPlayInTileNBTQuery(Object rawPacket, PacketContainer<WrappedPlayInTileNBTQuery> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
