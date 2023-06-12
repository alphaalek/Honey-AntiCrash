package me.alek.honey.netty.packetwrappers.login.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedLoginOutDisconnect extends WrappedPacket<WrappedLoginOutDisconnect> {

    public WrappedLoginOutDisconnect(Object rawPacket, PacketContainer<WrappedLoginOutDisconnect> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
