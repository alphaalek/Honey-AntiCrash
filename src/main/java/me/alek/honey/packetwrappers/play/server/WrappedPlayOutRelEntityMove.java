package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutRelEntityMove extends WrappedPacket<WrappedPlayOutRelEntityMove> {

    public WrappedPlayOutRelEntityMove(Object rawPacket, PacketContainer<WrappedPlayOutRelEntityMove> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
