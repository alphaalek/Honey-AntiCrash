package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutStopSound extends WrappedPacket<WrappedPlayOutStopSound> {

    public WrappedPlayOutStopSound(Object rawPacket, PacketContainer<WrappedPlayOutStopSound> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
