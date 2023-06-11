package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutServerDifficulty extends WrappedPacket<WrappedPlayOutServerDifficulty> {

    public WrappedPlayOutServerDifficulty(Object rawPacket, PacketContainer<WrappedPlayOutServerDifficulty> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
