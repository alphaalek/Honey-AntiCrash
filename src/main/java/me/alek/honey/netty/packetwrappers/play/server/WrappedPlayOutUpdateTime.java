package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutUpdateTime extends WrappedPacket<WrappedPlayOutUpdateTime> {

    public WrappedPlayOutUpdateTime(Object rawPacket, PacketContainer<WrappedPlayOutUpdateTime> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
