package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayInAutoRecipe extends WrappedPacket<WrappedPlayInAutoRecipe> {

    public WrappedPlayInAutoRecipe(Object rawPacket, PacketContainer<WrappedPlayInAutoRecipe> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
