package me.alek.honey.netty.packet.type;

import me.alek.honey.util.LazyInit;

public interface PacketType {

    Class<?> getNmsClass();

    LazyInit<Integer> getMinecraftID();

    String getKeyName1_20_5();

    int getHoneyID();

    PacketState getState();

    PacketBound getBound();
}
