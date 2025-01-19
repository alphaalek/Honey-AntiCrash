package me.alek.honey.netty.packetwrappers.impl.play.client;

import me.alek.honey.netty.packet.PacketContainer;

public class WrappedPlayInGround extends MovementPacketWrapper<WrappedPlayInGround> {

    public WrappedPlayInGround(Object rawPacket, PacketContainer<WrappedPlayInGround> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
