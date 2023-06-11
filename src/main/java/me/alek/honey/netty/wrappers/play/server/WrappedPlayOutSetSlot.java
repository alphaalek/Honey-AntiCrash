package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutSetSlot extends WrappedPacket<WrappedPlayOutSetSlot> {

    public WrappedPlayOutSetSlot(Object rawPacket, PacketContainer<WrappedPlayOutSetSlot> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
