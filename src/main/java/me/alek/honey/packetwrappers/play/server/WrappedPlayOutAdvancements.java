package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutAdvancements extends WrappedPacket<WrappedPlayOutAdvancements> {

    public WrappedPlayOutAdvancements(Object rawPacket, PacketContainer<WrappedPlayOutAdvancements> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
