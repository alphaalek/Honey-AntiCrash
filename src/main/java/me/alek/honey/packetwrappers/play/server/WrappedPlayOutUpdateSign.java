package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutUpdateSign extends WrappedPacket<WrappedPlayOutUpdateSign> {

    public WrappedPlayOutUpdateSign(Object rawPacket, PacketContainer<WrappedPlayOutUpdateSign> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
