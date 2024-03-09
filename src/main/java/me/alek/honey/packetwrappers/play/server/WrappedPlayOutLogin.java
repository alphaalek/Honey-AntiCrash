package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutLogin extends WrappedPacket<WrappedPlayOutLogin> {

    public WrappedPlayOutLogin(Object rawPacket, PacketContainer<WrappedPlayOutLogin> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
