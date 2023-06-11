package me.alek.honey.netty.wrappers.login.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedLoginOutSuccess extends WrappedPacket<WrappedLoginOutSuccess> {

    public WrappedLoginOutSuccess(Object rawPacket, PacketContainer<WrappedLoginOutSuccess> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
