package me.alek.honey.netty.wrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayInDifficultyChange extends WrappedPacket<WrappedPlayInDifficultyChange> {

    public WrappedPlayInDifficultyChange(Object rawPacket, PacketContainer<WrappedPlayInDifficultyChange> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
