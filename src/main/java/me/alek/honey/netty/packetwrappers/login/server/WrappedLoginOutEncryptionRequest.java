package me.alek.honey.netty.packetwrappers.login.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedLoginOutEncryptionRequest extends WrappedPacket<WrappedLoginOutEncryptionRequest> {

    public WrappedLoginOutEncryptionRequest(Object rawPacket, PacketContainer<WrappedLoginOutEncryptionRequest> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
