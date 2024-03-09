package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayInResourcePackStatus extends WrappedPacket<WrappedPlayInResourcePackStatus> {

    public WrappedPlayInResourcePackStatus(Object rawPacket, PacketContainer<WrappedPlayInResourcePackStatus> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
