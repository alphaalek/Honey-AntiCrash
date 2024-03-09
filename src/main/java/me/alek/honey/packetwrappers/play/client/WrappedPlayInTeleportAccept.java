package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayInTeleportAccept extends WrappedPacket<WrappedPlayInTeleportAccept> {

    public WrappedPlayInTeleportAccept(Object rawPacket, PacketContainer<WrappedPlayInTeleportAccept> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
