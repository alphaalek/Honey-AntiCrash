package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutLookAt extends WrappedPacket<WrappedPlayOutLookAt> {

    public WrappedPlayOutLookAt(Object rawPacket, PacketContainer<WrappedPlayOutLookAt> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
