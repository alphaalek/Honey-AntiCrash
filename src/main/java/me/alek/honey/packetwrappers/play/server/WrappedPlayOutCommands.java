package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutCommands extends WrappedPacket<WrappedPlayOutCommands> {

    public WrappedPlayOutCommands(Object rawPacket, PacketContainer<WrappedPlayOutCommands> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
