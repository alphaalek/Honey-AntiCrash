package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayInSetCommandBlock extends WrappedPacket<WrappedPlayInSetCommandBlock> {

    public WrappedPlayInSetCommandBlock(Object rawPacket, PacketContainer<WrappedPlayInSetCommandBlock> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
