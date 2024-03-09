package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutEntityHeadRotation extends WrappedPacket<WrappedPlayOutEntityHeadRotation> {

    public WrappedPlayOutEntityHeadRotation(Object rawPacket, PacketContainer<WrappedPlayOutEntityHeadRotation> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
