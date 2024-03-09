package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutEntityEffect extends WrappedPacket<WrappedPlayOutEntityEffect> {

    public WrappedPlayOutEntityEffect(Object rawPacket, PacketContainer<WrappedPlayOutEntityEffect> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
