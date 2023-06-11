package me.alek.honey.netty.wrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayInSetCreativeSlot extends WrappedPacket<WrappedPlayInSetCreativeSlot> {

    public WrappedPlayInSetCreativeSlot(Object rawPacket, PacketContainer<WrappedPlayInSetCreativeSlot> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
