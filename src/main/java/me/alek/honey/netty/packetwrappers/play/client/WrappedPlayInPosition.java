package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;

public class WrappedPlayInPosition extends MovementPacketWrapper<WrappedPlayInPosition> {

    public WrappedPlayInPosition(Object rawPacket, PacketContainer<WrappedPlayInPosition> packetContainer) {
        super(rawPacket, packetContainer);
    }

}
