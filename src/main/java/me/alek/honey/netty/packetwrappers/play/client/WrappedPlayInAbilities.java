package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayInAbilities extends WrappedPacket<WrappedPlayInAbilities> {

    public WrappedPlayInAbilities(Object rawPacket, PacketContainer<WrappedPlayInAbilities> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
