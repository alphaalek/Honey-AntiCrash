package me.alek.honey.netty.wrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayInSpectate extends WrappedPacket<WrappedPlayInSpectate> {

    public WrappedPlayInSpectate(Object rawPacket, PacketContainer<WrappedPlayInSpectate> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
