package me.alek.honey.netty.wrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayInResourcePackStatus extends WrappedPacket<WrappedPlayInResourcePackStatus> {

    public WrappedPlayInResourcePackStatus(Object rawPacket, PacketContainer<WrappedPlayInResourcePackStatus> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
