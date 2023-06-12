package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutSetCompression extends WrappedPacket<WrappedPlayOutSetCompression> {

    public WrappedPlayOutSetCompression(Object rawPacket, PacketContainer<WrappedPlayOutSetCompression> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
