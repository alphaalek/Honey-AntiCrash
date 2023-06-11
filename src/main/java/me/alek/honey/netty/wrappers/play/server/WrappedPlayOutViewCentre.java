package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutViewCentre extends WrappedPacket<WrappedPlayOutViewCentre> {

    public WrappedPlayOutViewCentre(Object rawPacket, PacketContainer<WrappedPlayOutViewCentre> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
