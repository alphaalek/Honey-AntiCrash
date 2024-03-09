package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutUpdateEntityNBT extends WrappedPacket<WrappedPlayOutUpdateEntityNBT> {

    public WrappedPlayOutUpdateEntityNBT(Object rawPacket, PacketContainer<WrappedPlayOutUpdateEntityNBT> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
