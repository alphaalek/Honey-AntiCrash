package me.alek.honey.packetwrappers.login.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedLoginInEncryptionBegin extends WrappedPacket<WrappedLoginInEncryptionBegin> {

    public WrappedLoginInEncryptionBegin(Object rawPacket, PacketContainer<WrappedLoginInEncryptionBegin> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
