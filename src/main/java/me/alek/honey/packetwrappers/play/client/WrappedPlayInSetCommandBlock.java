package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayInSetCommandBlock extends WrappedPacket<WrappedPlayInSetCommandBlock> {

    public WrappedPlayInSetCommandBlock(Object rawPacket, PacketContainer<WrappedPlayInSetCommandBlock> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
