package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutGameStateChange extends WrappedPacket<WrappedPlayOutGameStateChange> {

    public WrappedPlayOutGameStateChange(Object rawPacket, PacketContainer<WrappedPlayOutGameStateChange> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
