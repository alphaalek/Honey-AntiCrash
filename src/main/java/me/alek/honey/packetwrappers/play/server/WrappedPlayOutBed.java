package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutBed extends WrappedPacket<WrappedPlayOutBed> {

    public WrappedPlayOutBed(Object rawPacket, PacketContainer<WrappedPlayOutBed> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
