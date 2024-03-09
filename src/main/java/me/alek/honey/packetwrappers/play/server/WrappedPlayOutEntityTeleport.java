package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutEntityTeleport extends WrappedPacket<WrappedPlayOutEntityTeleport> {

    public WrappedPlayOutEntityTeleport(Object rawPacket, PacketContainer<WrappedPlayOutEntityTeleport> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
