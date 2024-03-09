package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutMapChunkBulk extends WrappedPacket<WrappedPlayOutMapChunkBulk> {

    public WrappedPlayOutMapChunkBulk(Object rawPacket, PacketContainer<WrappedPlayOutMapChunkBulk> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
