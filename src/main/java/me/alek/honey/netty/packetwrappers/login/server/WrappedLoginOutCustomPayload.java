package me.alek.honey.netty.packetwrappers.login.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedLoginOutCustomPayload extends WrappedPacket<WrappedLoginOutCustomPayload> {

    public WrappedLoginOutCustomPayload(Object rawPacket, PacketContainer<WrappedLoginOutCustomPayload> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
