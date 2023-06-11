package me.alek.honey.netty.wrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayInAdvancements extends WrappedPacket<WrappedPlayInAdvancements> {

    public WrappedPlayInAdvancements(Object rawPacket, PacketContainer<WrappedPlayInAdvancements> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
