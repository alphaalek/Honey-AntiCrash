package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutResourcePackSend extends WrappedPacket<WrappedPlayOutResourcePackSend> {

    public WrappedPlayOutResourcePackSend(Object rawPacket, PacketContainer<WrappedPlayOutResourcePackSend> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
