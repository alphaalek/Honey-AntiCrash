package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutScoreboardTeam extends WrappedPacket<WrappedPlayOutScoreboardTeam> {

    public WrappedPlayOutScoreboardTeam(Object rawPacket, PacketContainer<WrappedPlayOutScoreboardTeam> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
