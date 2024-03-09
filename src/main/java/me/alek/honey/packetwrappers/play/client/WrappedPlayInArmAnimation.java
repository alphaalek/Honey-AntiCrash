package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayInArmAnimation extends WrappedPacket<WrappedPlayInArmAnimation> {

    public WrappedPlayInArmAnimation(Object rawPacket, PacketContainer<WrappedPlayInArmAnimation> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
