package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packetwrappers.ObjectWrappedPacket;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInFlying;

public class WrappedPlayOutPosition extends ObjectWrappedPacket<WrappedPlayOutPosition> {

    public WrappedPlayOutPosition(Object rawPacket, PacketContainer<WrappedPlayOutPosition> packetContainer) {
        super(rawPacket, packetContainer);
    }

}
