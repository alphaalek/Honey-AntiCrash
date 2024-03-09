package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutUnloadChunk extends WrappedPacket<WrappedPlayOutUnloadChunk> {

    public WrappedPlayOutUnloadChunk(Object rawPacket, PacketContainer<WrappedPlayOutUnloadChunk> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
