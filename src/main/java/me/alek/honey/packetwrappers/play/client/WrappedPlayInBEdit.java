package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayInBEdit extends WrappedPacket<WrappedPlayInBEdit> {

    public WrappedPlayInBEdit(Object rawPacket, PacketContainer<WrappedPlayInBEdit> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
