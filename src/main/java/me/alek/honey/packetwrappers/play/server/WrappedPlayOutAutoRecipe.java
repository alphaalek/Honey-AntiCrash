package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutAutoRecipe extends WrappedPacket<WrappedPlayOutAutoRecipe> {

    public WrappedPlayOutAutoRecipe(Object rawPacket, PacketContainer<WrappedPlayOutAutoRecipe> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
