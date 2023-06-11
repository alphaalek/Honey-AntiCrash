package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutViewDistance extends WrappedPacket<WrappedPlayOutViewDistance> {

    public WrappedPlayOutViewDistance(Object rawPacket, PacketContainer<WrappedPlayOutViewDistance> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
