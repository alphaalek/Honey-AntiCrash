package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;

public class WrappedPlayInPositionLook extends WrappedPlayInFlying<WrappedPlayInPositionLook> {

    public WrappedPlayInPositionLook(Object rawPacket, PacketContainer<WrappedPlayInPositionLook> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
