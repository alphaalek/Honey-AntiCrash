package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutRecipes extends WrappedPacket<WrappedPlayOutRecipes> {

    public WrappedPlayOutRecipes(Object rawPacket, PacketContainer<WrappedPlayOutRecipes> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
