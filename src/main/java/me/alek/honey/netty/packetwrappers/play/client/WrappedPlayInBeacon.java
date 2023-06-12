package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayInBeacon extends WrappedPacket<WrappedPlayInBeacon> {

    public WrappedPlayInBeacon(Object rawPacket, PacketContainer<WrappedPlayInBeacon> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
