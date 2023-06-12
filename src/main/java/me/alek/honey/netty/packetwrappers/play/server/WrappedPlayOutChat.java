package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutChat extends WrappedPacket<WrappedPlayOutChat> {

    public WrappedPlayOutChat(Object rawPacket, PacketContainer<WrappedPlayOutChat> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
