package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayInTileNBTQuery extends WrappedPacket<WrappedPlayInTileNBTQuery> {

    public WrappedPlayInTileNBTQuery(Object rawPacket, PacketContainer<WrappedPlayInTileNBTQuery> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
