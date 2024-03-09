package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutExplosion extends WrappedPacket<WrappedPlayOutExplosion> {

    public WrappedPlayOutExplosion(Object rawPacket, PacketContainer<WrappedPlayOutExplosion> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
