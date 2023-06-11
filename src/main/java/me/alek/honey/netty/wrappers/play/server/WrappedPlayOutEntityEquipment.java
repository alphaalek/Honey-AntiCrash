package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutEntityEquipment extends WrappedPacket<WrappedPlayOutEntityEquipment> {

    public WrappedPlayOutEntityEquipment(Object rawPacket, PacketContainer<WrappedPlayOutEntityEquipment> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
