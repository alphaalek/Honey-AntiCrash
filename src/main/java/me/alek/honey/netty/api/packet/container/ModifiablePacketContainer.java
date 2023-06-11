package me.alek.honey.netty.api.packet.container;

import me.alek.honey.netty.api.packet.PacketModifier;

public interface ModifiablePacketContainer{

    PacketModifier<Double> getDoubles();

    PacketModifier<Long> getLongs();

    PacketModifier<Integer> getInts();

    PacketModifier<Short> getShorts();

    PacketModifier<Float> getFloats();

    PacketModifier<Byte> getBytes();

    PacketModifier<Boolean> getBooleans();

    PacketModifier<String> getStrings();

    PacketModifier<Object> getObjects(Class<?> target);

}
