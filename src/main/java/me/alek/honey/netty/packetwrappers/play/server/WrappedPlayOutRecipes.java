package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutRecipes extends WrappedPacket<WrappedPlayOutRecipes> {

    public WrappedPlayOutRecipes(Object rawPacket, PacketContainer<WrappedPlayOutRecipes> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
