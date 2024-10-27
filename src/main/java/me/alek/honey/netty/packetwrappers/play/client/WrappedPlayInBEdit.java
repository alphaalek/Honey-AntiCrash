package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packetwrappers.ObjectWrappedPacket;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayInBEdit extends ObjectWrappedPacket<WrappedPlayInBEdit> {

    public WrappedPlayInBEdit(Object rawPacket, PacketContainer<WrappedPlayInBEdit> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
