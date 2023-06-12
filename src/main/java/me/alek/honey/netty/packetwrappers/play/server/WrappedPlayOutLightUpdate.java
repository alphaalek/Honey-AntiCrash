package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutLightUpdate extends WrappedPacket<WrappedPlayOutLightUpdate> {

    public WrappedPlayOutLightUpdate(Object rawPacket, PacketContainer<WrappedPlayOutLightUpdate> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
