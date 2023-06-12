package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutBlockBreak extends WrappedPacket<WrappedPlayOutBlockBreak> {

    public WrappedPlayOutBlockBreak(Object rawPacket, PacketContainer<WrappedPlayOutBlockBreak> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
