package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;

public class WrappedPlayInVehicleMove extends WrappedPlayInFlying<WrappedPlayInVehicleMove> {

    public WrappedPlayInVehicleMove(Object rawPacket, PacketContainer<WrappedPlayInVehicleMove> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
