package me.alek.honey.netty.packet.bundle;

import me.alek.honey.netty.packet.type.PacketTypeEnum;

import java.util.Set;

public class PacketSaverWindow {

    private final int size;
    private final Set<PacketTypeEnum> packetTypes;

    public PacketSaverWindow(int size, Set<PacketTypeEnum> packetTypes) {
        this.size = size;
        this.packetTypes = packetTypes;
    }

    public int size() {
        return size;
    }

    public Set<PacketTypeEnum> getPacketTypes(){
        return packetTypes;
    }
}
