package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutStatistic extends WrappedPacket<WrappedPlayOutStatistic> {

    public WrappedPlayOutStatistic(Object rawPacket, PacketContainer<WrappedPlayOutStatistic> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
