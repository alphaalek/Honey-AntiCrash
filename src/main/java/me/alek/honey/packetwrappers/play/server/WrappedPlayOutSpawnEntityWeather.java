package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutSpawnEntityWeather extends WrappedPacket<WrappedPlayOutSpawnEntityWeather> {

    public WrappedPlayOutSpawnEntityWeather(Object rawPacket, PacketContainer<WrappedPlayOutSpawnEntityWeather> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
