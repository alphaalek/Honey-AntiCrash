package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayInRecipeDisplayed extends WrappedPacket<WrappedPlayInRecipeDisplayed> {

    public WrappedPlayInRecipeDisplayed(Object rawPacket, PacketContainer<WrappedPlayInRecipeDisplayed> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
