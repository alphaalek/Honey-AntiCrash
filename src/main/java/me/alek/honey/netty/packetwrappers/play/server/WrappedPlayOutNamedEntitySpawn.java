package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutNamedEntitySpawn extends WrappedPacket<WrappedPlayOutNamedEntitySpawn> {

    public WrappedPlayOutNamedEntitySpawn(Object rawPacket, PacketContainer<WrappedPlayOutNamedEntitySpawn> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
