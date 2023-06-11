package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutCamera extends WrappedPacket<WrappedPlayOutCamera> {

    public WrappedPlayOutCamera(Object rawPacket, PacketContainer<WrappedPlayOutCamera> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
