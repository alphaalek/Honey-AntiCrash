package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutAnimation extends WrappedPacket<WrappedPlayOutAnimation> {

    public WrappedPlayOutAnimation(Object rawPacket, PacketContainer<WrappedPlayOutAnimation> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
