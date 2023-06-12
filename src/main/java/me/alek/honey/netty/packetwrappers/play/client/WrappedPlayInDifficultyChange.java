package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayInDifficultyChange extends WrappedPacket<WrappedPlayInDifficultyChange> {

    public WrappedPlayInDifficultyChange(Object rawPacket, PacketContainer<WrappedPlayInDifficultyChange> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
