package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutCamera extends WrappedPacket<WrappedPlayOutCamera> {

    public WrappedPlayOutCamera(Object rawPacket, PacketContainer<WrappedPlayOutCamera> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
