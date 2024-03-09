package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayInJigsawGenerate extends WrappedPacket<WrappedPlayInJigsawGenerate> {

    public WrappedPlayInJigsawGenerate(Object rawPacket, PacketContainer<WrappedPlayInJigsawGenerate> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
