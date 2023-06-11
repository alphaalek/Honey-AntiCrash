package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutUnloadChunk extends WrappedPacket<WrappedPlayOutUnloadChunk> {

    public WrappedPlayOutUnloadChunk(Object rawPacket, PacketContainer<WrappedPlayOutUnloadChunk> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
