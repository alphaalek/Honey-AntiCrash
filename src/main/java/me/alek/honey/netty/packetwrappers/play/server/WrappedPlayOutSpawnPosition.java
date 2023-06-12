package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutSpawnPosition extends WrappedPacket<WrappedPlayOutSpawnPosition> {

    public WrappedPlayOutSpawnPosition(Object rawPacket, PacketContainer<WrappedPlayOutSpawnPosition> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
