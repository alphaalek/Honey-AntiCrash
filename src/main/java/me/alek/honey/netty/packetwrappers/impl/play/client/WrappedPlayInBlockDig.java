package me.alek.honey.netty.packetwrappers.impl.play.client;

import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.netty.packetwrappers.ObjectWrappedPacket;

public class WrappedPlayInBlockDig extends ObjectWrappedPacket<WrappedPlayInBlockDig> {

    public WrappedPlayInBlockDig(Object rawPacket, PacketContainer<WrappedPlayInBlockDig> packetContainer) {
        super(rawPacket, packetContainer);
    }

    public WrappedBlockPosition getBlockPosition() {
        return getContainer().getStructure().getBlockPositions().readField(0);
    }
}
