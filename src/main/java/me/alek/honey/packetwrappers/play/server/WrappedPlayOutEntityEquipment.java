package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutEntityEquipment extends WrappedPacket<WrappedPlayOutEntityEquipment> {

    public WrappedPlayOutEntityEquipment(Object rawPacket, PacketContainer<WrappedPlayOutEntityEquipment> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
