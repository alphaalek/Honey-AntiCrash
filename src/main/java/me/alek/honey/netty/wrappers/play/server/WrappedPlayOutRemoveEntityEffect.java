package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutRemoveEntityEffect extends WrappedPacket<WrappedPlayOutRemoveEntityEffect> {

    public WrappedPlayOutRemoveEntityEffect(Object rawPacket, PacketContainer<WrappedPlayOutRemoveEntityEffect> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
