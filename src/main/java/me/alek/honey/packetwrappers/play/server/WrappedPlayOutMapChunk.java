package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutMapChunk extends WrappedPacket<WrappedPlayOutMapChunk> {

    public WrappedPlayOutMapChunk(Object rawPacket, PacketContainer<WrappedPlayOutMapChunk> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
