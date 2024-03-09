package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayInSetCommandMinecart extends WrappedPacket<WrappedPlayInSetCommandMinecart> {

    public WrappedPlayInSetCommandMinecart(Object rawPacket, PacketContainer<WrappedPlayInSetCommandMinecart> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
