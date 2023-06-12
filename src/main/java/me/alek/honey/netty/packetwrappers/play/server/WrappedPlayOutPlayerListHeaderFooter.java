package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutPlayerListHeaderFooter extends WrappedPacket<WrappedPlayOutPlayerListHeaderFooter> {

    public WrappedPlayOutPlayerListHeaderFooter(Object rawPacket, PacketContainer<WrappedPlayOutPlayerListHeaderFooter> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
