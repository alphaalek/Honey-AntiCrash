package me.alek.honey.netty.wrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayInTransaction extends WrappedPacket<WrappedPlayInTransaction> {

    public WrappedPlayInTransaction(Object rawPacket, PacketContainer<WrappedPlayInTransaction> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
