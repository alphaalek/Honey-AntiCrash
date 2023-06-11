package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutUpdateEntityNBT extends WrappedPacket<WrappedPlayOutUpdateEntityNBT> {

    public WrappedPlayOutUpdateEntityNBT(Object rawPacket, PacketContainer<WrappedPlayOutUpdateEntityNBT> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
