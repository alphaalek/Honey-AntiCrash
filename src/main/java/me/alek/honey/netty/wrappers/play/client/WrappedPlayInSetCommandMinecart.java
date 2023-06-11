package me.alek.honey.netty.wrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayInSetCommandMinecart extends WrappedPacket<WrappedPlayInSetCommandMinecart> {

    public WrappedPlayInSetCommandMinecart(Object rawPacket, PacketContainer<WrappedPlayInSetCommandMinecart> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
