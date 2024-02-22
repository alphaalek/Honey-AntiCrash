package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayInEnchantItem extends WrappedPacket<WrappedPlayInEnchantItem> {

    public WrappedPlayInEnchantItem(Object rawPacket, PacketContainer<WrappedPlayInEnchantItem> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
