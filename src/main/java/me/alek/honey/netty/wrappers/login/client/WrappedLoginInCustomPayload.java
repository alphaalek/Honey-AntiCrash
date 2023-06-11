package me.alek.honey.netty.wrappers.login.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedLoginInCustomPayload extends WrappedPacket<WrappedLoginInCustomPayload> {

    public WrappedLoginInCustomPayload(Object rawPacket, PacketContainer<WrappedLoginInCustomPayload> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
