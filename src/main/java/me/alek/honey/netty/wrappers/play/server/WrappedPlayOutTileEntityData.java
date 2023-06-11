package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutTileEntityData extends WrappedPacket<WrappedPlayOutTileEntityData> {

    public WrappedPlayOutTileEntityData(Object rawPacket, PacketContainer<WrappedPlayOutTileEntityData> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
