package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutLogin extends WrappedPacket<WrappedPlayOutLogin> {

    public WrappedPlayOutLogin(Object rawPacket, PacketContainer<WrappedPlayOutLogin> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
