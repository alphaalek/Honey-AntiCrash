package me.alek.honey.packetwrappers.login.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedLoginOutCustomPayload extends WrappedPacket<WrappedLoginOutCustomPayload> {

    public WrappedLoginOutCustomPayload(Object rawPacket, PacketContainer<WrappedLoginOutCustomPayload> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
