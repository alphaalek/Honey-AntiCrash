package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayInTransaction extends WrappedPacket<WrappedPlayInTransaction> {

    public WrappedPlayInTransaction(Object rawPacket, PacketContainer<WrappedPlayInTransaction> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
