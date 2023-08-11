package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayInBlockDig extends WrappedPacket<WrappedPlayInBlockDig> {

    public WrappedPlayInBlockDig(Object rawPacket, PacketContainer<WrappedPlayInBlockDig> packetContainer) {
        super(rawPacket, packetContainer);
    }

    public WrappedBlockPosition getBlockPosition() {
        return getContainer().getBlockPositions().readField(0);
    }
}
