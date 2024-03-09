package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutEntityMetadata extends WrappedPacket<WrappedPlayOutEntityMetadata> {

    public WrappedPlayOutEntityMetadata(Object rawPacket, PacketContainer<WrappedPlayOutEntityMetadata> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
