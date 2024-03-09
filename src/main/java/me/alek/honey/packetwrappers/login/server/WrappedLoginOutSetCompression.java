package me.alek.honey.packetwrappers.login.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedLoginOutSetCompression extends WrappedPacket<WrappedLoginOutSetCompression> {

    public WrappedLoginOutSetCompression(Object rawPacket, PacketContainer<WrappedLoginOutSetCompression> packetContainer) {
        super(rawPacket, packetContainer);
    }

    public int getCompressionSize() {
        return getContainer().getInts().readField(0);
    }
}
