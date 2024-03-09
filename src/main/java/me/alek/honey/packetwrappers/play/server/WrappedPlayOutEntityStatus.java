package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutEntityStatus extends WrappedPacket<WrappedPlayOutEntityStatus> {

    public WrappedPlayOutEntityStatus(Object rawPacket, PacketContainer<WrappedPlayOutEntityStatus> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
