package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutEntityLook extends WrappedPacket<WrappedPlayOutEntityLook> {

    public WrappedPlayOutEntityLook(Object rawPacket, PacketContainer<WrappedPlayOutEntityLook> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
