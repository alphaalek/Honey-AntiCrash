package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutEntityDestroy extends WrappedPacket<WrappedPlayOutEntityDestroy> {

    public WrappedPlayOutEntityDestroy(Object rawPacket, PacketContainer<WrappedPlayOutEntityDestroy> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
