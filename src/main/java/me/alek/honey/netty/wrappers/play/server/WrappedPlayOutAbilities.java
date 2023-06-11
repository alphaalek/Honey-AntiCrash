package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutAbilities extends WrappedPacket<WrappedPlayOutAbilities> {

    public WrappedPlayOutAbilities(Object rawPacket, PacketContainer<WrappedPlayOutAbilities> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
