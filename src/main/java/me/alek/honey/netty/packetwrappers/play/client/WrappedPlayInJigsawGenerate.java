package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayInJigsawGenerate extends WrappedPacket<WrappedPlayInJigsawGenerate> {

    public WrappedPlayInJigsawGenerate(Object rawPacket, PacketContainer<WrappedPlayInJigsawGenerate> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
