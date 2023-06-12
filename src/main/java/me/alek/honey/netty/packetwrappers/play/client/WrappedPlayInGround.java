package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;

public class WrappedPlayInGround extends WrappedPlayInFlying<WrappedPlayInGround> {

    public WrappedPlayInGround(Object rawPacket, PacketContainer<WrappedPlayInGround> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
