package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayInUseEntity extends WrappedPacket<WrappedPlayInUseEntity> {
    public WrappedPlayInUseEntity(Object rawPacket, PacketContainer<WrappedPlayInUseEntity> packetContainer) {
        super(rawPacket,packetContainer);
    }
}
