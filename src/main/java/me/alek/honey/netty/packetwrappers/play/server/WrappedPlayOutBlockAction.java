package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutBlockAction extends WrappedPacket<WrappedPlayOutBlockAction> {

    public WrappedPlayOutBlockAction(Object rawPacket, PacketContainer<WrappedPlayOutBlockAction> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
