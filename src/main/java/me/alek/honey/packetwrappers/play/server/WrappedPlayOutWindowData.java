package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutWindowData extends WrappedPacket<WrappedPlayOutWindowData> {

    public WrappedPlayOutWindowData(Object rawPacket, PacketContainer<WrappedPlayOutWindowData> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
