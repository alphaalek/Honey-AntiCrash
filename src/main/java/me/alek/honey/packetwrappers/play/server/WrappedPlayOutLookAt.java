package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutLookAt extends WrappedPacket<WrappedPlayOutLookAt> {

    public WrappedPlayOutLookAt(Object rawPacket, PacketContainer<WrappedPlayOutLookAt> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
