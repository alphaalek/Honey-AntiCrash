package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayInUseEntity extends WrappedPacket<WrappedPlayInUseEntity> {

    public WrappedPlayInUseEntity(Object rawPacket, PacketContainer<WrappedPlayInUseEntity> packetContainer) {
        super(rawPacket,packetContainer);
    }
}
