package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutPosition extends WrappedPacket<WrappedPlayOutPosition> {

    public WrappedPlayOutPosition(Object rawPacket, PacketContainer<WrappedPlayOutPosition> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
