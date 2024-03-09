package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutSpawnEntityPainting extends WrappedPacket<WrappedPlayOutSpawnEntityPainting> {

    public WrappedPlayOutSpawnEntityPainting(Object rawPacket, PacketContainer<WrappedPlayOutSpawnEntityPainting> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
