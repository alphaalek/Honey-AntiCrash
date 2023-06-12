package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutEntityLook extends WrappedPacket<WrappedPlayOutEntityLook> {

    public WrappedPlayOutEntityLook(Object rawPacket, PacketContainer<WrappedPlayOutEntityLook> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
