package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayInEntityAction extends WrappedPacket<WrappedPlayInEntityAction> {

    public WrappedPlayInEntityAction(Object rawPacket, PacketContainer<WrappedPlayInEntityAction> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
