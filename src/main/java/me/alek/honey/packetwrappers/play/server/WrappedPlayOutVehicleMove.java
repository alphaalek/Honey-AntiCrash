package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutVehicleMove extends WrappedPacket<WrappedPlayOutVehicleMove> {

    public WrappedPlayOutVehicleMove(Object rawPacket, PacketContainer<WrappedPlayOutVehicleMove> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
