package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutHeldItemSlot extends WrappedPacket<WrappedPlayOutHeldItemSlot> {

    public WrappedPlayOutHeldItemSlot(Object rawPacket, PacketContainer<WrappedPlayOutHeldItemSlot> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
