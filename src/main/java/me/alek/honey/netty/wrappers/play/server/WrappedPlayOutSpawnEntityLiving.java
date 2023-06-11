package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutSpawnEntityLiving extends WrappedPacket<WrappedPlayOutSpawnEntityLiving> {

    public WrappedPlayOutSpawnEntityLiving(Object rawPacket, PacketContainer<WrappedPlayOutSpawnEntityLiving> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
