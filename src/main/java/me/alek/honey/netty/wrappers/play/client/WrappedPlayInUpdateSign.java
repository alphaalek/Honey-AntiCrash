package me.alek.honey.netty.wrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayInUpdateSign extends WrappedPacket<WrappedPlayInUpdateSign> {

    public WrappedPlayInUpdateSign(Object rawPacket, PacketContainer<WrappedPlayInUpdateSign> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
