package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutEntityDestroy extends WrappedPacket<WrappedPlayOutEntityDestroy> {

    public WrappedPlayOutEntityDestroy(Object rawPacket, PacketContainer<WrappedPlayOutEntityDestroy> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
