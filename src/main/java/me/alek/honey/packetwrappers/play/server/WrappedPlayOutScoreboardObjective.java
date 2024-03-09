package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutScoreboardObjective extends WrappedPacket<WrappedPlayOutScoreboardObjective> {

    public WrappedPlayOutScoreboardObjective(Object rawPacket, PacketContainer<WrappedPlayOutScoreboardObjective> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
