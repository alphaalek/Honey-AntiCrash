package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayInArmAnimation extends WrappedPacket<WrappedPlayInArmAnimation> {

    public WrappedPlayInArmAnimation(Object rawPacket, PacketContainer<WrappedPlayInArmAnimation> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
