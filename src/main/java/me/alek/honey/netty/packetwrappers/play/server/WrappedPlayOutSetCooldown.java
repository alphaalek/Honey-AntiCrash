package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutSetCooldown extends WrappedPacket<WrappedPlayOutSetCooldown> {

    public WrappedPlayOutSetCooldown(Object rawPacket, PacketContainer<WrappedPlayOutSetCooldown> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
