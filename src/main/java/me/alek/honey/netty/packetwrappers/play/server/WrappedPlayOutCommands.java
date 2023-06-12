package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutCommands extends WrappedPacket<WrappedPlayOutCommands> {

    public WrappedPlayOutCommands(Object rawPacket, PacketContainer<WrappedPlayOutCommands> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
