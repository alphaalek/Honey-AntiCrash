package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutBlockAction extends WrappedPacket<WrappedPlayOutBlockAction> {

    public WrappedPlayOutBlockAction(Object rawPacket, PacketContainer<WrappedPlayOutBlockAction> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
