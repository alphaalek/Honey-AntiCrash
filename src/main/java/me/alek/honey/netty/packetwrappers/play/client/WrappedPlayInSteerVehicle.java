package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayInSteerVehicle extends WrappedPacket<WrappedPlayInSteerVehicle> {

    public WrappedPlayInSteerVehicle(Object rawPacket, PacketContainer<WrappedPlayInSteerVehicle> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
