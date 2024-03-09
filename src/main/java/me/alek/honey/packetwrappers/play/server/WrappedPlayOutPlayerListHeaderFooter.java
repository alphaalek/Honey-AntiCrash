package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutPlayerListHeaderFooter extends WrappedPacket<WrappedPlayOutPlayerListHeaderFooter> {

    public WrappedPlayOutPlayerListHeaderFooter(Object rawPacket, PacketContainer<WrappedPlayOutPlayerListHeaderFooter> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
