package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutSpawnEntityPainting extends WrappedPacket<WrappedPlayOutSpawnEntityPainting> {

    public WrappedPlayOutSpawnEntityPainting(Object rawPacket, PacketContainer<WrappedPlayOutSpawnEntityPainting> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
