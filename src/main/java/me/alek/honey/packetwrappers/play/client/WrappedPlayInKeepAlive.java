package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayInKeepAlive extends WrappedPacket<WrappedPlayInKeepAlive> {

    public WrappedPlayInKeepAlive(Object rawPacket, PacketContainer<WrappedPlayInKeepAlive> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
