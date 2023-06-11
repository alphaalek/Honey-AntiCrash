package me.alek.honey.netty.wrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayInPickItem extends WrappedPacket<WrappedPlayInPickItem> {

    public WrappedPlayInPickItem(Object rawPacket, PacketContainer<WrappedPlayInPickItem> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
