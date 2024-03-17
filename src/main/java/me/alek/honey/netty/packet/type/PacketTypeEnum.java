package me.alek.honey.netty.packet.type;

public interface PacketTypeEnum {

    Class<?> getNmsClass();

    int getPacketId();

    PacketState getState();

    PacketBound getBound();

}
