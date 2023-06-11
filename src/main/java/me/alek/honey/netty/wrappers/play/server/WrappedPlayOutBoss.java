package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutBoss extends WrappedPacket<WrappedPlayOutBoss> {

    public WrappedPlayOutBoss(Object rawPacket, PacketContainer<WrappedPlayOutBoss> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
