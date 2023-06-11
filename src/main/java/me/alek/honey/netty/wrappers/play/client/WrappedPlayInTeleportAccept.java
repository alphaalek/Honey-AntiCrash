package me.alek.honey.netty.wrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayInTeleportAccept extends WrappedPacket<WrappedPlayInTeleportAccept> {

    public WrappedPlayInTeleportAccept(Object rawPacket, PacketContainer<WrappedPlayInTeleportAccept> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
