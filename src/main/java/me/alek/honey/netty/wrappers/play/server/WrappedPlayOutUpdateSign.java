package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutUpdateSign extends WrappedPacket<WrappedPlayOutUpdateSign> {

    public WrappedPlayOutUpdateSign(Object rawPacket, PacketContainer<WrappedPlayOutUpdateSign> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
