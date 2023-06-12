package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutTags extends WrappedPacket<WrappedPlayOutTags> {

    public WrappedPlayOutTags(Object rawPacket, PacketContainer<WrappedPlayOutTags> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
