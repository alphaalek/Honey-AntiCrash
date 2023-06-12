package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;

public class WrappedPlayInLook extends WrappedPlayInFlying<WrappedPlayInLook> {

    public WrappedPlayInLook(Object rawPacket, PacketContainer<WrappedPlayInLook> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
