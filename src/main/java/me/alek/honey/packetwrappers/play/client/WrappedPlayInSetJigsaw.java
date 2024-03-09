package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayInSetJigsaw extends WrappedPacket<WrappedPlayInSetJigsaw> {

    public WrappedPlayInSetJigsaw(Object rawPacket, PacketContainer<WrappedPlayInSetJigsaw> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
