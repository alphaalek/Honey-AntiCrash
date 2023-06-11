package me.alek.honey.netty.wrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayInRecipeDisplayed extends WrappedPacket<WrappedPlayInRecipeDisplayed> {

    public WrappedPlayInRecipeDisplayed(Object rawPacket, PacketContainer<WrappedPlayInRecipeDisplayed> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
