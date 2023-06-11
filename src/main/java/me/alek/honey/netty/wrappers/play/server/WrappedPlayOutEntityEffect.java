package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutEntityEffect extends WrappedPacket<WrappedPlayOutEntityEffect> {

    public WrappedPlayOutEntityEffect(Object rawPacket, PacketContainer<WrappedPlayOutEntityEffect> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
