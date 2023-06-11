package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutRecipes extends WrappedPacket<WrappedPlayOutRecipes> {

    public WrappedPlayOutRecipes(Object rawPacket, PacketContainer<WrappedPlayOutRecipes> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
