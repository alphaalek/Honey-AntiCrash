package me.alek.honey.netty.wrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayOutSpawnEntityWeather extends WrappedPacket<WrappedPlayOutSpawnEntityWeather> {

    public WrappedPlayOutSpawnEntityWeather(Object rawPacket, PacketContainer<WrappedPlayOutSpawnEntityWeather> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
