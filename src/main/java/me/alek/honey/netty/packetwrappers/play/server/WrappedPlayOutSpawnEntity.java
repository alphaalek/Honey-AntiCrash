package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutSpawnEntity extends WrappedPacket<WrappedPlayOutSpawnEntity> {
    public WrappedPlayOutSpawnEntity(Object rawPacket, PacketContainer<WrappedPlayOutSpawnEntity> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
