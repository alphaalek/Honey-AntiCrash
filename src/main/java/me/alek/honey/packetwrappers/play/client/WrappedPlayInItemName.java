package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayInItemName extends WrappedPacket<WrappedPlayInItemName> {

    public WrappedPlayInItemName(Object rawPacket, PacketContainer<WrappedPlayInItemName> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
