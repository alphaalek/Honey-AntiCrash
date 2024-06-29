package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutDisconnect extends WrappedPacket<WrappedPlayOutDisconnect> {

    public WrappedPlayOutDisconnect(Object rawPacket, PacketContainer<WrappedPlayOutDisconnect> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
