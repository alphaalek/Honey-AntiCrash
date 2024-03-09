package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutRespawn extends WrappedPacket<WrappedPlayOutRespawn> {

    public WrappedPlayOutRespawn(Object rawPacket, PacketContainer<WrappedPlayOutRespawn> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
