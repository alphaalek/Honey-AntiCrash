package me.alek.honey.netty.packet.type;

import me.alek.honey.util.LazyInit;

public interface PacketTypeEnum {

    Class<?> getNmsClass();

    LazyInit<Integer> getMinecraftID();

    int getHoneyID();

    PacketState getState();

    PacketBound getBound();

    default boolean isCommonState() {
        return false;
    }

}
