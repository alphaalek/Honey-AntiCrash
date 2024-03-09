package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInFlying;

public class WrappedPlayOutPosition extends WrappedPlayInFlying<WrappedPlayOutPosition> {

    public WrappedPlayOutPosition(Object rawPacket, PacketContainer<WrappedPlayOutPosition> packetContainer) {
        super(rawPacket, packetContainer);
    }

}
