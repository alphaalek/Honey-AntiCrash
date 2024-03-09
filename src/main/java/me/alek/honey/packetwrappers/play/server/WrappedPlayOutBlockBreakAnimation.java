package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutBlockBreakAnimation extends WrappedPacket<WrappedPlayOutBlockBreakAnimation> {

    public WrappedPlayOutBlockBreakAnimation(Object rawPacket, PacketContainer<WrappedPlayOutBlockBreakAnimation> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
