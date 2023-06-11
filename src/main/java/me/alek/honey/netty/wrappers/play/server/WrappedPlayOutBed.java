package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutBed extends WrappedPacket<WrappedPlayOutBed> {

    public WrappedPlayOutBed(Object rawPacket, PacketContainer<WrappedPlayOutBed> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
