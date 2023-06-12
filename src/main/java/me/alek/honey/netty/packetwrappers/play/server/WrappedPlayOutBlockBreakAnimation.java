package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutBlockBreakAnimation extends WrappedPacket<WrappedPlayOutBlockBreakAnimation> {

    public WrappedPlayOutBlockBreakAnimation(Object rawPacket, PacketContainer<WrappedPlayOutBlockBreakAnimation> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
