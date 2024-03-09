package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutEntityVelocity extends WrappedPacket<WrappedPlayOutEntityVelocity> {

    public WrappedPlayOutEntityVelocity(Object rawPacket, PacketContainer<WrappedPlayOutEntityVelocity> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
