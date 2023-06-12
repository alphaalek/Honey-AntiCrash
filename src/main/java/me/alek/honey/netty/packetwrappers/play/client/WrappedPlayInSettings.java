package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayInSettings extends WrappedPacket<WrappedPlayInSettings> {

    public WrappedPlayInSettings(Object rawPacket, PacketContainer<WrappedPlayInSettings> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
