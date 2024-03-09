package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutSetCompression extends WrappedPacket<WrappedPlayOutSetCompression> {

    public WrappedPlayOutSetCompression(Object rawPacket, PacketContainer<WrappedPlayOutSetCompression> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
