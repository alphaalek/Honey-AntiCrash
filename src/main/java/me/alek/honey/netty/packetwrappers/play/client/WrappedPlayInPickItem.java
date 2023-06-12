package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayInPickItem extends WrappedPacket<WrappedPlayInPickItem> {

    public WrappedPlayInPickItem(Object rawPacket, PacketContainer<WrappedPlayInPickItem> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
