package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutEntitySound extends WrappedPacket<WrappedPlayOutEntitySound> {

    public WrappedPlayOutEntitySound(Object rawPacket, PacketContainer<WrappedPlayOutEntitySound> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
