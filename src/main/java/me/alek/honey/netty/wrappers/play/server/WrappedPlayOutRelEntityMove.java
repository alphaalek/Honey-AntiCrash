package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutRelEntityMove extends WrappedPacket<WrappedPlayOutRelEntityMove> {

    public WrappedPlayOutRelEntityMove(Object rawPacket, PacketContainer<WrappedPlayOutRelEntityMove> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
