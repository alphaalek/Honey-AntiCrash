package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutEntity extends WrappedPacket<WrappedPlayOutEntity> {

    public WrappedPlayOutEntity(Object rawPacket, PacketContainer<WrappedPlayOutEntity> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
