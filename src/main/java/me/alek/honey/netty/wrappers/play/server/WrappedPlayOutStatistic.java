package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutStatistic extends WrappedPacket<WrappedPlayOutStatistic> {

    public WrappedPlayOutStatistic(Object rawPacket, PacketContainer<WrappedPlayOutStatistic> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
