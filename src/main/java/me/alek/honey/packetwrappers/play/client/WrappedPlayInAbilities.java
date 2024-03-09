package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayInAbilities extends WrappedPacket<WrappedPlayInAbilities> {

    public WrappedPlayInAbilities(Object rawPacket, PacketContainer<WrappedPlayInAbilities> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
