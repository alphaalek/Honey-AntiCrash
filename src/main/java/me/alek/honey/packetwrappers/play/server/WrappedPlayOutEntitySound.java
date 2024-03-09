package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutEntitySound extends WrappedPacket<WrappedPlayOutEntitySound> {

    public WrappedPlayOutEntitySound(Object rawPacket, PacketContainer<WrappedPlayOutEntitySound> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
