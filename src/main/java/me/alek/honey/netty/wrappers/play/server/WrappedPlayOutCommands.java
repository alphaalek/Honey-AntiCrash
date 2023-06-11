package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutCommands extends WrappedPacket<WrappedPlayOutCommands> {

    public WrappedPlayOutCommands(Object rawPacket, PacketContainer<WrappedPlayOutCommands> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
