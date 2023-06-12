package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutGameStateChange extends WrappedPacket<WrappedPlayOutGameStateChange> {

    public WrappedPlayOutGameStateChange(Object rawPacket, PacketContainer<WrappedPlayOutGameStateChange> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
