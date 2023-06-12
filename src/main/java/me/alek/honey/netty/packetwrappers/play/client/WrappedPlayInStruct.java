package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayInStruct extends WrappedPacket<WrappedPlayInStruct> {

    public WrappedPlayInStruct(Object rawPacket, PacketContainer<WrappedPlayInStruct> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
