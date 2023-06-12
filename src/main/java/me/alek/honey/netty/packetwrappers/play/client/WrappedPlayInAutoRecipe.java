package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayInAutoRecipe extends WrappedPacket<WrappedPlayInAutoRecipe> {

    public WrappedPlayInAutoRecipe(Object rawPacket, PacketContainer<WrappedPlayInAutoRecipe> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
