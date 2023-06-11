package me.alek.honey.netty.wrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayInGround extends WrappedPlayInFlying<WrappedPlayInGround> {

    public WrappedPlayInGround(Object rawPacket, PacketContainer<WrappedPlayInGround> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
