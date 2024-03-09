package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutSpawnPosition extends WrappedPacket<WrappedPlayOutSpawnPosition> {

    public WrappedPlayOutSpawnPosition(Object rawPacket, PacketContainer<WrappedPlayOutSpawnPosition> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
