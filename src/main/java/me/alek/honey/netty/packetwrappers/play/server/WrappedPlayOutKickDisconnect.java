package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutKickDisconnect extends WrappedPacket<WrappedPlayOutKickDisconnect> {

    public WrappedPlayOutKickDisconnect(Object rawPacket, PacketContainer<WrappedPlayOutKickDisconnect> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
