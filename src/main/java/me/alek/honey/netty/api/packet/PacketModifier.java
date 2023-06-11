package me.alek.honey.netty.api.packet;

import java.util.List;

public interface PacketModifier<T> {

    PacketModifier<T> write(int index, T value);

    T read(int index);

    List<T> readValues();

    boolean has(int index);
}
