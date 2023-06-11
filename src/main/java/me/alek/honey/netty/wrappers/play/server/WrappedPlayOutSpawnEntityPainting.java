package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutSpawnEntityPainting extends WrappedPacket<WrappedPlayOutSpawnEntityPainting> {

    public WrappedPlayOutSpawnEntityPainting(Object rawPacket, PacketContainer<WrappedPlayOutSpawnEntityPainting> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
