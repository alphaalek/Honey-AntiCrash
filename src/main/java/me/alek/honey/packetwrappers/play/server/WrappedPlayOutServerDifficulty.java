package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutServerDifficulty extends WrappedPacket<WrappedPlayOutServerDifficulty> {

    public WrappedPlayOutServerDifficulty(Object rawPacket, PacketContainer<WrappedPlayOutServerDifficulty> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
