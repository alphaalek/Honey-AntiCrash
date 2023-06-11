package me.alek.honey.netty.wrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayInSetJigsaw extends WrappedPacket<WrappedPlayInSetJigsaw> {

    public WrappedPlayInSetJigsaw(Object rawPacket, PacketContainer<WrappedPlayInSetJigsaw> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
