package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutOpenWindowMerchant extends WrappedPacket<WrappedPlayOutOpenWindowMerchant> {

    public WrappedPlayOutOpenWindowMerchant(Object rawPacket, PacketContainer<WrappedPlayOutOpenWindowMerchant> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
