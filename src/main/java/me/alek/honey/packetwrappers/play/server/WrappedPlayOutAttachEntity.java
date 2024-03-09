package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutAttachEntity extends WrappedPacket<WrappedPlayOutAttachEntity> {

    public WrappedPlayOutAttachEntity(Object rawPacket, PacketContainer<WrappedPlayOutAttachEntity> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
