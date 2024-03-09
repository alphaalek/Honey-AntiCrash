package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayInPickItem extends WrappedPacket<WrappedPlayInPickItem> {

    public WrappedPlayInPickItem(Object rawPacket, PacketContainer<WrappedPlayInPickItem> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
