package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutTitle extends WrappedPacket<WrappedPlayOutTitle> {

    public WrappedPlayOutTitle(Object rawPacket, PacketContainer<WrappedPlayOutTitle> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
