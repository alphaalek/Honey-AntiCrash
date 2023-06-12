package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayInSetCommandMinecart extends WrappedPacket<WrappedPlayInSetCommandMinecart> {

    public WrappedPlayInSetCommandMinecart(Object rawPacket, PacketContainer<WrappedPlayInSetCommandMinecart> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
