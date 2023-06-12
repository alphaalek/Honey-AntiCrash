package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutExplosion extends WrappedPacket<WrappedPlayOutExplosion> {

    public WrappedPlayOutExplosion(Object rawPacket, PacketContainer<WrappedPlayOutExplosion> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
