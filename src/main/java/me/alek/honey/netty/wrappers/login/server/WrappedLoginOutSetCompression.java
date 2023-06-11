package me.alek.honey.netty.wrappers.login.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedLoginOutSetCompression extends WrappedPacket<WrappedLoginOutSetCompression> {

    public WrappedLoginOutSetCompression(Object rawPacket, PacketContainer<WrappedLoginOutSetCompression> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
