package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutHeldItemSlot extends WrappedPacket<WrappedPlayOutHeldItemSlot> {

    public WrappedPlayOutHeldItemSlot(Object rawPacket, PacketContainer<WrappedPlayOutHeldItemSlot> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
