package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayInSpectate extends WrappedPacket<WrappedPlayInSpectate> {

    public WrappedPlayInSpectate(Object rawPacket, PacketContainer<WrappedPlayInSpectate> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
