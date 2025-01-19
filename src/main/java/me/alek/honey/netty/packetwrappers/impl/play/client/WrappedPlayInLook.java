package me.alek.honey.netty.packetwrappers.impl.play.client;

import me.alek.honey.netty.packet.PacketContainer;

public class WrappedPlayInLook extends MovementPacketWrapper<WrappedPlayInLook> {

    public WrappedPlayInLook(Object rawPacket, PacketContainer<WrappedPlayInLook> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
