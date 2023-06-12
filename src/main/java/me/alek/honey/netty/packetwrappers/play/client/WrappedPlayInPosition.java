package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;

public class WrappedPlayInPosition extends WrappedPlayInFlying<WrappedPlayInPosition> {

    public WrappedPlayInPosition(Object rawPacket, PacketContainer<WrappedPlayInPosition> packetContainer) {
        super(rawPacket, packetContainer);
    }

}
