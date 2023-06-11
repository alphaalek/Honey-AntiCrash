package me.alek.honey.netty.wrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayInWindowClick extends WrappedPacket<WrappedPlayInWindowClick> {

    public WrappedPlayInWindowClick(Object rawPacket, PacketContainer<WrappedPlayInWindowClick> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
