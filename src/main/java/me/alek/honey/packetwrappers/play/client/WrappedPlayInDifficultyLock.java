package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayInDifficultyLock extends WrappedPacket<WrappedPlayInDifficultyLock> {

    public WrappedPlayInDifficultyLock(Object rawPacket, PacketContainer<WrappedPlayInDifficultyLock> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
