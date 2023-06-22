package me.alek.honey.netty.api.packet;

import java.util.List;

public interface IStructureModifier<T> {

    IStructureModifier<T> writeField(int index, T value);

    T readField(int index);

    T invokeMethod(int index, Object... params);

    List<T> readFieldValues();

    boolean hasField(int index);

    boolean hasMethod(int index);

    int fieldSize();

    int methodSize();
}
