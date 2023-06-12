package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayInEntityAction extends WrappedPacket<WrappedPlayInEntityAction> {

    public WrappedPlayInEntityAction(Object rawPacket, PacketContainer<WrappedPlayInEntityAction> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
