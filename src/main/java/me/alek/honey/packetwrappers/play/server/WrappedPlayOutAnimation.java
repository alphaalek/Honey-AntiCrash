package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutAnimation extends WrappedPacket<WrappedPlayOutAnimation> {

    public WrappedPlayOutAnimation(Object rawPacket, PacketContainer<WrappedPlayOutAnimation> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
