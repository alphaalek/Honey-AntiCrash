package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutMount extends WrappedPacket<WrappedPlayOutMount> {

    public WrappedPlayOutMount(Object rawPacket, PacketContainer<WrappedPlayOutMount> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
