package me.alek.honey.netty.packetwrappers.login.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedLoginInEncryptionResponse extends WrappedPacket<WrappedLoginInEncryptionResponse> {

    public WrappedLoginInEncryptionResponse(Object rawPacket, PacketContainer<WrappedLoginInEncryptionResponse> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
