package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutAttachEntity extends WrappedPacket<WrappedPlayOutAttachEntity> {

    public WrappedPlayOutAttachEntity(Object rawPacket, PacketContainer<WrappedPlayOutAttachEntity> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
