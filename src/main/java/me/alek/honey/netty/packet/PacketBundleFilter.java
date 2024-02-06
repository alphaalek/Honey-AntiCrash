package me.alek.honey.netty.packet;

import me.alek.honey.netty.packet.type.PacketTypeEnum;

public interface PacketBundleFilter {

    int size();

    PacketTypeEnum[] getAcceptedPacketTypes();
}
