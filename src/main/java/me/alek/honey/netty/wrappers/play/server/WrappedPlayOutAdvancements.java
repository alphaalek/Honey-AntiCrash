package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutAdvancements extends WrappedPacket<WrappedPlayOutAdvancements> {

    public WrappedPlayOutAdvancements(Object rawPacket, PacketContainer<WrappedPlayOutAdvancements> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
