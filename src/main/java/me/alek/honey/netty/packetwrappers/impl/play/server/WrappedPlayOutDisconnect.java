package me.alek.honey.netty.packetwrappers.impl.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packetwrappers.ObjectWrappedPacket;

public class WrappedPlayOutDisconnect extends ObjectWrappedPacket<WrappedPlayOutDisconnect> {

    public WrappedPlayOutDisconnect(Object rawPacket, PacketContainer<WrappedPlayOutDisconnect> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
