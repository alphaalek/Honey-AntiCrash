package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutWorldParticles extends WrappedPacket<WrappedPlayOutWorldParticles> {

    public WrappedPlayOutWorldParticles(Object rawPacket, PacketContainer<WrappedPlayOutWorldParticles> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
