package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayInRecipeSettings extends WrappedPacket<WrappedPlayInRecipeSettings> {

    public WrappedPlayInRecipeSettings(Object rawPacket, PacketContainer<WrappedPlayInRecipeSettings> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
