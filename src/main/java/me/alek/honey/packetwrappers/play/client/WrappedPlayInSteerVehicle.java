package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayInSteerVehicle extends WrappedPacket<WrappedPlayInSteerVehicle> {

    public WrappedPlayInSteerVehicle(Object rawPacket, PacketContainer<WrappedPlayInSteerVehicle> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
