package me.alek.honey.packetwrappers.login.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedLoginOutEncryptionRequest extends WrappedPacket<WrappedLoginOutEncryptionRequest> {

    public WrappedLoginOutEncryptionRequest(Object rawPacket, PacketContainer<WrappedLoginOutEncryptionRequest> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
