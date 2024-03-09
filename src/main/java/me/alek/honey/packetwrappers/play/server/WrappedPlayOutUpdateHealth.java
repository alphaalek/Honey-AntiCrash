package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutUpdateHealth extends WrappedPacket<WrappedPlayOutUpdateHealth> {

    public WrappedPlayOutUpdateHealth(Object rawPacket, PacketContainer<WrappedPlayOutUpdateHealth> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
