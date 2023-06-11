package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutVehicleMove extends WrappedPacket<WrappedPlayOutVehicleMove> {

    public WrappedPlayOutVehicleMove(Object rawPacket, PacketContainer<WrappedPlayOutVehicleMove> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
