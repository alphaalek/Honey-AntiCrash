package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutMapChunk extends WrappedPacket<WrappedPlayOutMapChunk> {

    public WrappedPlayOutMapChunk(Object rawPacket, PacketContainer<WrappedPlayOutMapChunk> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
