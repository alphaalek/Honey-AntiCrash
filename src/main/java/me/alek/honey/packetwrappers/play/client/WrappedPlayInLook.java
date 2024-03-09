package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;

public class WrappedPlayInLook extends WrappedPlayInFlying<WrappedPlayInLook> {

    public WrappedPlayInLook(Object rawPacket, PacketContainer<WrappedPlayInLook> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
