package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutStopSound extends WrappedPacket<WrappedPlayOutStopSound> {

    public WrappedPlayOutStopSound(Object rawPacket, PacketContainer<WrappedPlayOutStopSound> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
