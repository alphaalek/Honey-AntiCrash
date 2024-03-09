package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutTileEntityData extends WrappedPacket<WrappedPlayOutTileEntityData> {

    public WrappedPlayOutTileEntityData(Object rawPacket, PacketContainer<WrappedPlayOutTileEntityData> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
