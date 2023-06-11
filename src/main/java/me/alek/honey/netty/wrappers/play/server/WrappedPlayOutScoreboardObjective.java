package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutScoreboardObjective extends WrappedPacket<WrappedPlayOutScoreboardObjective> {

    public WrappedPlayOutScoreboardObjective(Object rawPacket, PacketContainer<WrappedPlayOutScoreboardObjective> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
