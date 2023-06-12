package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayInAdvancements extends WrappedPacket<WrappedPlayInAdvancements> {

    public WrappedPlayInAdvancements(Object rawPacket, PacketContainer<WrappedPlayInAdvancements> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
