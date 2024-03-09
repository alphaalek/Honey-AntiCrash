package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayInDifficultyChange extends WrappedPacket<WrappedPlayInDifficultyChange> {

    public WrappedPlayInDifficultyChange(Object rawPacket, PacketContainer<WrappedPlayInDifficultyChange> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
