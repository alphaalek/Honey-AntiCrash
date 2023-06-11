package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutRespawn extends WrappedPacket<WrappedPlayOutRespawn> {

    public WrappedPlayOutRespawn(Object rawPacket, PacketContainer<WrappedPlayOutRespawn> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
