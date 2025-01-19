package me.alek.honey.netty.packetwrappers.impl.play.server;

import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.ObjectWrappedPacket;

public class WrappedPlayOutChat extends ObjectWrappedPacket<WrappedPlayOutChat> {

    public WrappedPlayOutChat(Object rawPacket, PacketContainer<WrappedPlayOutChat> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
