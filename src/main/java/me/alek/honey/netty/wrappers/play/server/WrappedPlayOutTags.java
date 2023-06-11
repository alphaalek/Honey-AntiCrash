package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutTags extends WrappedPacket<WrappedPlayOutTags> {

    public WrappedPlayOutTags(Object rawPacket, PacketContainer<WrappedPlayOutTags> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
