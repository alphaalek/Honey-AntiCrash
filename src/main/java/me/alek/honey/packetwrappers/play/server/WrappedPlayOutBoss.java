package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutBoss extends WrappedPacket<WrappedPlayOutBoss> {

    public WrappedPlayOutBoss(Object rawPacket, PacketContainer<WrappedPlayOutBoss> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
