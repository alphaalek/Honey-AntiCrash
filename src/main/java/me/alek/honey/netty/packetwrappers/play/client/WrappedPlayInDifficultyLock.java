package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayInDifficultyLock extends WrappedPacket<WrappedPlayInDifficultyLock> {

    public WrappedPlayInDifficultyLock(Object rawPacket, PacketContainer<WrappedPlayInDifficultyLock> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
