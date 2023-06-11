package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutEntityVelocity extends WrappedPacket<WrappedPlayOutEntityVelocity> {

    public WrappedPlayOutEntityVelocity(Object rawPacket, PacketContainer<WrappedPlayOutEntityVelocity> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
