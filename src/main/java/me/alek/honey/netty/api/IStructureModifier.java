package me.alek.honey.netty.api;

import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.converters.JavaConverter;
import me.alek.honey.util.reflection.FieldAccessor;
import me.alek.honey.util.reflection.MethodInvoker;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;

public interface IStructureModifier<T> {

    Object getHandle();

    ReflectStructure<?, T> getStructure();

    JavaConverter<?, T> getConverter();

    FieldAccessor<T> getField(int index);

    <O> FieldAccessor<T> getField(AtomicReference<FieldAccessor<O>> fieldReference, String name, int index);

    <O> FieldAccessor<T> getField(AtomicReference<FieldAccessor<O>> fieldReference, BiPredicate<AtomicInteger, FieldAccessor<?>> filter);

    IStructureModifier<T> writeField(int index, T value);

    <O> IStructureModifier<T> writeField(AtomicReference<FieldAccessor<O>> fieldReference, String name, int index, T value);
    <O> IStructureModifier<T> writeField(AtomicReference<FieldAccessor<O>> fieldReference, BiPredicate<AtomicInteger, FieldAccessor<?>> filter, T value);

    T readField(int index);

    <K, V, R> T readFieldAndApplyMapping(int index);

    <O> T readField(AtomicReference<FieldAccessor<O>> fieldReference, String name, int index);

    <O> T readField(AtomicReference<FieldAccessor<O>> fieldReference, BiPredicate<AtomicInteger, FieldAccessor<?>> filter);

    <O> MethodInvoker<T> getMethod(AtomicReference<MethodInvoker<O>> methodReference, int index, Class<?>... params);

    <O> MethodInvoker<T> getMethod(AtomicReference<MethodInvoker<O>> methodReference, String name, int index, Class<?>... params);

    <O> MethodInvoker<T> getMethod(AtomicReference<MethodInvoker<O>> methodReference, BiPredicate<AtomicInteger, MethodInvoker<?>> filter, Class<?>... params);

    <O> T invokeMethod(AtomicReference<MethodInvoker<O>> methodReference, BiPredicate<AtomicInteger, MethodInvoker<?>> filter, Object... params);

    <O> T invokeMethod(AtomicReference<MethodInvoker<O>> methodReference, int index, Object... params);

    <O> T invokeMethod(AtomicReference<MethodInvoker<O>> methodReference, String name, int index, Object... params);

    <O> T invokeMethodWithException(AtomicReference<MethodInvoker<O>> methodReference, BiPredicate<AtomicInteger, MethodInvoker<?>> filter, Object... params) throws Exception;

    <O> T invokeMethodWithException(AtomicReference<MethodInvoker<O>> methodReference, int index, Object... params) throws Exception;

    <O> T invokeMethodWithException(AtomicReference<MethodInvoker<O>> methodReference, String name, int index, Object... params) throws Exception;

    Collection<T> readFieldValues();

    Map<String, T> readFieldEntries();

    boolean hasField(int index);

    boolean hasField(String name, int index);

    boolean hasMethod(int index);

    boolean hasMethod(String name, int index);

    int fieldSize();

    int methodSize();

    <O> Collection<FieldAccessor<O>> getAccessors();

    <O> Collection<MethodInvoker<O>> getInvokers();

}
