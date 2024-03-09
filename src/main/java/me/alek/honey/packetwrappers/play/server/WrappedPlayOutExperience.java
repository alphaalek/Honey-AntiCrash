package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutExperience extends WrappedPacket<WrappedPlayOutExperience> {

    public WrappedPlayOutExperience(Object rawPacket, PacketContainer<WrappedPlayOutExperience> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
