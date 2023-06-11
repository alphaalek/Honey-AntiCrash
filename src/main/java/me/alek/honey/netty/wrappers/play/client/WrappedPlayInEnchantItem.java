package me.alek.honey.netty.wrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayInEnchantItem extends WrappedPacket<WrappedPlayInEnchantItem> {

    public WrappedPlayInEnchantItem(Object rawPacket, PacketContainer<WrappedPlayInEnchantItem> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
