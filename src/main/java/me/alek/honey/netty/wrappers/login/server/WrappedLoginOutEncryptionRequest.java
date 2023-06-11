package me.alek.honey.netty.wrappers.login.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedLoginOutEncryptionRequest extends WrappedPacket<WrappedLoginOutEncryptionRequest> {

    public WrappedLoginOutEncryptionRequest(Object rawPacket, PacketContainer<WrappedLoginOutEncryptionRequest> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
