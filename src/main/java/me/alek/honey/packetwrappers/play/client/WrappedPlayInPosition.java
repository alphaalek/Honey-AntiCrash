package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;

public class WrappedPlayInPosition extends WrappedPlayInFlying<WrappedPlayInPosition> {

    public WrappedPlayInPosition(Object rawPacket, PacketContainer<WrappedPlayInPosition> packetContainer) {
        super(rawPacket, packetContainer);
    }

}
