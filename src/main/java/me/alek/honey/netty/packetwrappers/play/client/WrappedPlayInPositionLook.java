package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;

public class WrappedPlayInPositionLook extends WrappedPlayInFlying<WrappedPlayInPositionLook> {

    public WrappedPlayInPositionLook(Object rawPacket, PacketContainer<WrappedPlayInPositionLook> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
