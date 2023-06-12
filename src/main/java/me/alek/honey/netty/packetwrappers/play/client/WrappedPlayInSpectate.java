package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayInSpectate extends WrappedPacket<WrappedPlayInSpectate> {

    public WrappedPlayInSpectate(Object rawPacket, PacketContainer<WrappedPlayInSpectate> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
