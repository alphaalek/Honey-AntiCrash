package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutExperience extends WrappedPacket<WrappedPlayOutExperience> {

    public WrappedPlayOutExperience(Object rawPacket, PacketContainer<WrappedPlayOutExperience> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
