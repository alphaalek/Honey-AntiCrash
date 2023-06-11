package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutWorldParticles extends WrappedPacket<WrappedPlayOutWorldParticles> {

    public WrappedPlayOutWorldParticles(Object rawPacket, PacketContainer<WrappedPlayOutWorldParticles> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
