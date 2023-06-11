package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutEntityHeadRotation extends WrappedPacket<WrappedPlayOutEntityHeadRotation> {

    public WrappedPlayOutEntityHeadRotation(Object rawPacket, PacketContainer<WrappedPlayOutEntityHeadRotation> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
