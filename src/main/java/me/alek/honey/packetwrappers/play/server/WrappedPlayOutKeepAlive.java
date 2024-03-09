package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutKeepAlive extends WrappedPacket<WrappedPlayOutKeepAlive> {

    public WrappedPlayOutKeepAlive(Object rawPacket, PacketContainer<WrappedPlayOutKeepAlive> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
