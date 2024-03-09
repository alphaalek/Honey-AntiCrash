package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutChat extends WrappedPacket<WrappedPlayOutChat> {

    public WrappedPlayOutChat(Object rawPacket, PacketContainer<WrappedPlayOutChat> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
