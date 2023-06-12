package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutUpdateEntityNBT extends WrappedPacket<WrappedPlayOutUpdateEntityNBT> {

    public WrappedPlayOutUpdateEntityNBT(Object rawPacket, PacketContainer<WrappedPlayOutUpdateEntityNBT> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
