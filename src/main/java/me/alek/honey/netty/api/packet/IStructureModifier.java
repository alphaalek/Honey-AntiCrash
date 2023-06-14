package me.alek.honey.netty.api.packet;

import java.util.List;

public interface IStructureModifier<T> {

    IStructureModifier<T> write(int index, T value);

    T read(int index);

    List<T> readValues();

    boolean has(int index);

    int size();
}
