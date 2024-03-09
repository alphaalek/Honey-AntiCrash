package me.alek.honey.packetwrappers.login.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedLoginOutSuccess extends WrappedPacket<WrappedLoginOutSuccess> {

    public WrappedLoginOutSuccess(Object rawPacket, PacketContainer<WrappedLoginOutSuccess> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
