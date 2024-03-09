package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutTitle extends WrappedPacket<WrappedPlayOutTitle> {

    public WrappedPlayOutTitle(Object rawPacket, PacketContainer<WrappedPlayOutTitle> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
