package me.alek.honey.netty.wrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayInRecipeSettings extends WrappedPacket<WrappedPlayInRecipeSettings> {

    public WrappedPlayInRecipeSettings(Object rawPacket, PacketContainer<WrappedPlayInRecipeSettings> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
