package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayInItemName extends WrappedPacket<WrappedPlayInItemName> {

    public WrappedPlayInItemName(Object rawPacket, PacketContainer<WrappedPlayInItemName> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
