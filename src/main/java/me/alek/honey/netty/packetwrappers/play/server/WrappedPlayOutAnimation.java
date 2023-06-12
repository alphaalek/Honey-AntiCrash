package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutAnimation extends WrappedPacket<WrappedPlayOutAnimation> {

    public WrappedPlayOutAnimation(Object rawPacket, PacketContainer<WrappedPlayOutAnimation> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
