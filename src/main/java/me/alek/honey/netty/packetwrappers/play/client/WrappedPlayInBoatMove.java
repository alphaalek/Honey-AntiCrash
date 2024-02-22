package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayInBoatMove extends WrappedPacket<WrappedPlayInBoatMove> {

    public WrappedPlayInBoatMove(Object rawPacket, PacketContainer<WrappedPlayInBoatMove> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
