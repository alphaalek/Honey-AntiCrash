package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutScoreboardDisplayObjective extends WrappedPacket<WrappedPlayOutScoreboardDisplayObjective> {

    public WrappedPlayOutScoreboardDisplayObjective(Object rawPacket, PacketContainer<WrappedPlayOutScoreboardDisplayObjective> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
