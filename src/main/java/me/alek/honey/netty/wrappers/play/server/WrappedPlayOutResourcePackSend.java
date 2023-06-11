package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutResourcePackSend extends WrappedPacket<WrappedPlayOutResourcePackSend> {

    public WrappedPlayOutResourcePackSend(Object rawPacket, PacketContainer<WrappedPlayOutResourcePackSend> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
