package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayInStruct extends WrappedPacket<WrappedPlayInStruct> {

    public WrappedPlayInStruct(Object rawPacket, PacketContainer<WrappedPlayInStruct> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
