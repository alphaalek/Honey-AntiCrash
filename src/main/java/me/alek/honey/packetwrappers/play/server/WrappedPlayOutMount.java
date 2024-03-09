package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutMount extends WrappedPacket<WrappedPlayOutMount> {

    public WrappedPlayOutMount(Object rawPacket, PacketContainer<WrappedPlayOutMount> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
