package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutAbilities extends WrappedPacket<WrappedPlayOutAbilities> {

    public WrappedPlayOutAbilities(Object rawPacket, PacketContainer<WrappedPlayOutAbilities> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
