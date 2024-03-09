package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutBlockBreak extends WrappedPacket<WrappedPlayOutBlockBreak> {

    public WrappedPlayOutBlockBreak(Object rawPacket, PacketContainer<WrappedPlayOutBlockBreak> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
