package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayInRecipeDisplayed extends WrappedPacket<WrappedPlayInRecipeDisplayed> {

    public WrappedPlayInRecipeDisplayed(Object rawPacket, PacketContainer<WrappedPlayInRecipeDisplayed> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
