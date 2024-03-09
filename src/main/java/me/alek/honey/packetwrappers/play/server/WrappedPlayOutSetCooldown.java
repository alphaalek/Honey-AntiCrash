package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutSetCooldown extends WrappedPacket<WrappedPlayOutSetCooldown> {

    public WrappedPlayOutSetCooldown(Object rawPacket, PacketContainer<WrappedPlayOutSetCooldown> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
