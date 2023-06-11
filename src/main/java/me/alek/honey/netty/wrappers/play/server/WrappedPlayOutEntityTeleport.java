package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutEntityTeleport extends WrappedPacket<WrappedPlayOutEntityTeleport> {

    public WrappedPlayOutEntityTeleport(Object rawPacket, PacketContainer<WrappedPlayOutEntityTeleport> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
