package me.alek.honey.packetwrappers.login.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedLoginInCustomPayload extends WrappedPacket<WrappedLoginInCustomPayload> {

    public WrappedLoginInCustomPayload(Object rawPacket, PacketContainer<WrappedLoginInCustomPayload> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
