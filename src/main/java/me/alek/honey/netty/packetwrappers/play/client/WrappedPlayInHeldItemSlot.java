package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packetwrappers.ObjectWrappedPacket;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayInHeldItemSlot extends ObjectWrappedPacket<WrappedPlayInHeldItemSlot> {

    public WrappedPlayInHeldItemSlot(Object rawPacket, PacketContainer<WrappedPlayInHeldItemSlot> packetContainer) {
        super(rawPacket, packetContainer);
    }

    public int getSlot() {
        return getContainer().getStructure().getInts().readField(0);
    }
}
