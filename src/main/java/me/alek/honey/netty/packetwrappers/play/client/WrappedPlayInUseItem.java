package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayInUseItem extends WrappedPacket<WrappedPlayInUseItem> {

    public WrappedPlayInUseItem(Object rawPacket, PacketContainer<WrappedPlayInUseItem> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
