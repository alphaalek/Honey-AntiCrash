package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutSpawnEntityLiving extends WrappedPacket<WrappedPlayOutSpawnEntityLiving> {

    public WrappedPlayOutSpawnEntityLiving(Object rawPacket, PacketContainer<WrappedPlayOutSpawnEntityLiving> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
