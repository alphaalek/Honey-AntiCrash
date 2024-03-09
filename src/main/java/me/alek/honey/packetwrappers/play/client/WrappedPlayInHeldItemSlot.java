package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayInHeldItemSlot extends WrappedPacket<WrappedPlayInHeldItemSlot> {

    public WrappedPlayInHeldItemSlot(Object rawPacket, PacketContainer<WrappedPlayInHeldItemSlot> packetContainer) {
        super(rawPacket, packetContainer);
    }

    public int getSlot() {
        return getContainer().getInts().readField(0);
    }
}
