package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutMapChunkBulk extends WrappedPacket<WrappedPlayOutMapChunkBulk> {

    public WrappedPlayOutMapChunkBulk(Object rawPacket, PacketContainer<WrappedPlayOutMapChunkBulk> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
