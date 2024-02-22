package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutPlayerInfo extends WrappedPacket<WrappedPlayOutPlayerInfo> {

    public WrappedPlayOutPlayerInfo(Object rawPacket, PacketContainer<WrappedPlayOutPlayerInfo> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
