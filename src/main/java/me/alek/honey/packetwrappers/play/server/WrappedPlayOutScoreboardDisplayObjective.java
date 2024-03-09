package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutScoreboardDisplayObjective extends WrappedPacket<WrappedPlayOutScoreboardDisplayObjective> {

    public WrappedPlayOutScoreboardDisplayObjective(Object rawPacket, PacketContainer<WrappedPlayOutScoreboardDisplayObjective> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
