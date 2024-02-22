package me.alek.honey.netty.api;

import me.alek.honey.netty.structure.converters.JavaConverter;
import me.alek.honey.netty.utility.FieldAccessor;
import me.alek.honey.netty.utility.MethodInvoker;

import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;

public interface IStructureModifier<T> {

    Object getHandle();

    JavaConverter<?, T> getConverter();

    IStructureModifier<T> writeField(int index, T value);

    IStructureModifier<T> writeField(String name, int index, T value);

    T readField(int index);

    <K, V, R> T readFieldAndApplyMapping(int index);

    T readField(String name, int index);

    T invokeMethod(Predicate<MethodInvoker<?>> filter, Object... params);

    T invokeMethod(String name);

    T invokeMethod(int index, Object... params);

    T invokeMethod(String name, int index, Object... params);

    T invokeMethodWithException(Predicate<MethodInvoker<?>> filter, Object... params) throws Exception;

    T invokeMethodWithException(String name) throws Exception;

    T invokeMethodWithException(int index, Object... params) throws Exception;

    T invokeMethodWithException(String name, int index, Object... params) throws Exception;

    Collection<T> readFieldValues();

    Map<String, T> readFieldEntries();

    boolean hasField(int index);

    boolean hasField(String name, int index);

    boolean hasMethod(int index);

    boolean hasMethod(String name, int index);

    int fieldSize();

    int methodSize();

    Collection<FieldAccessor<?>> getAccessors();

    Collection<MethodInvoker<?>> getInvokers();

}
