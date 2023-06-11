package me.alek.honey.netty.wrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayInBEdit extends WrappedPacket<WrappedPlayInBEdit> {

    public WrappedPlayInBEdit(Object rawPacket, PacketContainer<WrappedPlayInBEdit> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
