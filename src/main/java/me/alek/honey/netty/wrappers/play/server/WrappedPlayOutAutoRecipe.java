package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutAutoRecipe extends WrappedPacket<WrappedPlayOutAutoRecipe> {

    public WrappedPlayOutAutoRecipe(Object rawPacket, PacketContainer<WrappedPlayOutAutoRecipe> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
