package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayInSettings extends WrappedPacket<WrappedPlayInSettings> {

    public WrappedPlayInSettings(Object rawPacket, PacketContainer<WrappedPlayInSettings> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
