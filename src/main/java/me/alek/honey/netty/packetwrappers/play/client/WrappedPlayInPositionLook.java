package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;

public class WrappedPlayInPositionLook extends MovementPacketWrapper<WrappedPlayInPositionLook> {

    public WrappedPlayInPositionLook(Object rawPacket, PacketContainer<WrappedPlayInPositionLook> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
