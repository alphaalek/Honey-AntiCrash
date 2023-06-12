package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutScoreboardScore extends WrappedPacket<WrappedPlayOutScoreboardScore> {

    public WrappedPlayOutScoreboardScore(Object rawPacket, PacketContainer<WrappedPlayOutScoreboardScore> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
