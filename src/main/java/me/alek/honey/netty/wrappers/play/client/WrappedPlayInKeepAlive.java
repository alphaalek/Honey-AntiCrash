package me.alek.honey.netty.wrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayInKeepAlive extends WrappedPacket<WrappedPlayInKeepAlive> {

    public WrappedPlayInKeepAlive(Object rawPacket, PacketContainer<WrappedPlayInKeepAlive> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
