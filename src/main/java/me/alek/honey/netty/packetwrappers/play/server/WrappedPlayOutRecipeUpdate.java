package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutRecipeUpdate extends WrappedPacket<WrappedPlayOutRecipeUpdate> {

    public WrappedPlayOutRecipeUpdate(Object rawPacket, PacketContainer<WrappedPlayOutRecipeUpdate> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
