package me.alek.honey.packetwrappers.login.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedLoginOutDisconnect extends WrappedPacket<WrappedLoginOutDisconnect> {

    public WrappedLoginOutDisconnect(Object rawPacket, PacketContainer<WrappedLoginOutDisconnect> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
