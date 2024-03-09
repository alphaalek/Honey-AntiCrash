package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutLightUpdate extends WrappedPacket<WrappedPlayOutLightUpdate> {

    public WrappedPlayOutLightUpdate(Object rawPacket, PacketContainer<WrappedPlayOutLightUpdate> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
