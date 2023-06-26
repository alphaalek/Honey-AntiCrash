package me.alek.honey.netty.api.packet;

import me.alek.honey.netty.structure.converters.JavaConverter;

import java.util.Collection;
import java.util.List;

public interface IStructureModifier<T> {

    JavaConverter<?, T> getConverter();

    IStructureModifier<T> writeField(int index, T value);

    IStructureModifier<T> writeField(String name, int index, T value);

    T readField(int index);

    T readField(String name, int index);

    T invokeMethod(int index, Object... params);

    T invokeMethod(String name, int index, Object... params);

    List<T> readFieldValues();

    boolean hasField(int index);

    boolean hasField(String name, int index);

    boolean hasMethod(int index);

    boolean hasMethod(String name, int index);

    int fieldSize();

    int methodSize();

    Collection<?> getAccessors();

    Collection<?> getInvokers();

}
