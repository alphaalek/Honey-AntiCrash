package me.alek.honey.netty.packetwrappers.impl.play.client;

import me.alek.honey.netty.packet.PacketContainer;

public class WrappedPlayInPositionLook extends MovementPacketWrapper<WrappedPlayInPositionLook> {

    public WrappedPlayInPositionLook(Object rawPacket, PacketContainer<WrappedPlayInPositionLook> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
