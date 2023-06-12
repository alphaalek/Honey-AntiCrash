package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayInVehicleMove extends WrappedPacket<WrappedPlayInVehicleMove> {

    public WrappedPlayInVehicleMove(Object rawPacket, PacketContainer<WrappedPlayInVehicleMove> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
