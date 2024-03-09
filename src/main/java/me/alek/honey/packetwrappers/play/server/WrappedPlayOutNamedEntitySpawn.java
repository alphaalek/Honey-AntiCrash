package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutNamedEntitySpawn extends WrappedPacket<WrappedPlayOutNamedEntitySpawn> {

    public WrappedPlayOutNamedEntitySpawn(Object rawPacket, PacketContainer<WrappedPlayOutNamedEntitySpawn> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
