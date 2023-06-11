package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutEntityMetadata extends WrappedPacket<WrappedPlayOutEntityMetadata> {

    public WrappedPlayOutEntityMetadata(Object rawPacket, PacketContainer<WrappedPlayOutEntityMetadata> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
