package me.alek.honey.util;

import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.util.reflection.MethodInvoker;

import java.util.concurrent.atomic.AtomicReference;

public interface Provider<T, P extends Provider<T, P>> {

    <O> P adaptRemote(AtomicReference<MethodInvoker<O>> cache, Object other);

    interface ProviderNoInput<T> extends Provider<T, ProviderNoInput<T>> {

        T get();

        default <O> ProviderNoInput<T> adaptRemote(AtomicReference<MethodInvoker<O>> cache, Object other) {
            IStructureModifier<T> structure = ReflectStructureCache.<T>acquireStructure(other.getClass())
                    .withTarget(other);
            return () -> structure.invokeMethod(cache, 0);
        }
    }

    interface ProviderOneInput<T, I1> extends Provider<T, ProviderOneInput<T, I1>> {

        T get(I1 i1);

        default <O> ProviderOneInput<T, I1> adaptRemote(AtomicReference<MethodInvoker<O>> cache, Object other) {
            IStructureModifier<T> structure = ReflectStructureCache.<T>acquireStructure(other.getClass())
                    .withTarget(other);
            return i1 -> structure.invokeMethod(cache, 0, i1);
        }
    }

    interface ProviderTwoInput<T, I1, I2> extends Provider<T, ProviderTwoInput<T, I1, I2>> {

        T get(I1 i1, I2 i2);

        default <O> ProviderTwoInput<T, I1, I2> adaptRemote(AtomicReference<MethodInvoker<O>> cache, Object other) {
            IStructureModifier<T> structure = ReflectStructureCache.<T>acquireStructure(other.getClass())
                    .withTarget(other);
            return (i1, i2) -> structure.invokeMethod(cache, 0, i1, i2);
        }
    }

    interface ProviderGenericInput<T> extends Provider<T, ProviderGenericInput<T>> {

        T get(Array<Object> array);

        default <O> ProviderGenericInput<T> adaptRemote(AtomicReference<MethodInvoker<O>> cache, Object other) {
            IStructureModifier<T> structure = ReflectStructureCache.<T>acquireStructure(other.getClass())
                    .withTarget(other);
            return array -> structure.invokeMethod(cache, 0, array);
        }
    }
}
