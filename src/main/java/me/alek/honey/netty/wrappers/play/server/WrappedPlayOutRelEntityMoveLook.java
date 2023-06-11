package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutRelEntityMoveLook extends WrappedPacket<WrappedPlayOutRelEntityMoveLook> {

    public WrappedPlayOutRelEntityMoveLook(Object rawPacket, PacketContainer<WrappedPlayOutRelEntityMoveLook> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
