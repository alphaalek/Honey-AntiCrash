package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutKeepAlive extends WrappedPacket<WrappedPlayOutKeepAlive> {

    public WrappedPlayOutKeepAlive(Object rawPacket, PacketContainer<WrappedPlayOutKeepAlive> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
