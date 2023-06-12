package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayInSetJigsaw extends WrappedPacket<WrappedPlayInSetJigsaw> {

    public WrappedPlayInSetJigsaw(Object rawPacket, PacketContainer<WrappedPlayInSetJigsaw> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
