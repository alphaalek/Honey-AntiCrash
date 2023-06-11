package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutTransaction extends WrappedPacket<WrappedPlayOutTransaction> {

    public WrappedPlayOutTransaction(Object rawPacket, PacketContainer<WrappedPlayOutTransaction> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
