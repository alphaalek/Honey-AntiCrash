package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutScoreboardTeam extends WrappedPacket<WrappedPlayOutScoreboardTeam> {

    public WrappedPlayOutScoreboardTeam(Object rawPacket, PacketContainer<WrappedPlayOutScoreboardTeam> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
