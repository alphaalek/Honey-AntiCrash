package me.alek.honey.netty.api.packet.container;

import me.alek.honey.netty.api.packet.PacketModifier;

import java.util.UUID;

public interface ModifiablePacketContainer{

    PacketModifier<Double> getDoubles();

    PacketModifier<Long> getLongs();

    PacketModifier<Integer> getInts();

    PacketModifier<Short> getShorts();

    PacketModifier<Float> getFloats();

    PacketModifier<Byte> getBytes();

    PacketModifier<Boolean> getBooleans();

    PacketModifier<String> getStrings();

    PacketModifier<Object> getObjects();

    PacketModifier<UUID> getUUIDS();

    PacketModifier<String[]> getStringArrays();

    PacketModifier<long[]> getLongArrays();

    PacketModifier<int[]> getIntArrays();

    PacketModifier<short[]> getShortArrays();

    PacketModifier<byte[]> getByteArrays();

    PacketModifier<Object> getItems();

    PacketModifier<Object> getDataSerializers();

    <T> PacketModifier<T> getObjects(Class<T> target);

}
