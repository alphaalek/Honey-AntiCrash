package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutEntityTeleport extends WrappedPacket<WrappedPlayOutEntityTeleport> {

    public WrappedPlayOutEntityTeleport(Object rawPacket, PacketContainer<WrappedPlayOutEntityTeleport> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
