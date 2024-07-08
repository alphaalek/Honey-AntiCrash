package me.alek.honey.util;

import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.structure.CachedReflectStructureProvider;
import me.alek.honey.util.reflection.MethodInvoker;

import java.util.concurrent.atomic.AtomicReference;

public interface ProviderAdapter<T, P extends Provider<T, P, A>, A extends ProviderAdapter<T, P, A>> {

    <O> P adaptRemote(AtomicReference<MethodInvoker<O>> cache, Object other);

    interface ProviderNoInput<T> extends ProviderAdapter<T, Provider.ProviderNoInput<T>, ProviderNoInput<T>> {

        default <O> Provider.ProviderNoInput<T> adaptRemote(AtomicReference<MethodInvoker<O>> cache, Object other) {
            IStructureModifier<T> structure = CachedReflectStructureProvider.<T>acquireStructure(other.getClass())
                    .withTarget(other);
            return () -> structure.invokeMethod(cache, 0);
        }
    }

    interface ProviderOneInput<T, I1> extends ProviderAdapter<T, Provider.ProviderOneInput<T, I1>, ProviderOneInput<T, I1>> {

        default <O> Provider.ProviderOneInput<T, I1> adaptRemote(AtomicReference<MethodInvoker<O>> cache, Object other) {
            IStructureModifier<T> structure = CachedReflectStructureProvider.<T>acquireStructure(other.getClass())
                    .withTarget(other);
            return (i1) -> structure.invokeMethod(cache, 0, i1);
        }
    }

    interface ProviderTwoInput<T, I1, I2> extends ProviderAdapter<T, Provider.ProviderTwoInput<T, I1, I2>, ProviderTwoInput<T, I1, I2>> {

        default <O> Provider.ProviderTwoInput<T, I1, I2> adaptRemote(AtomicReference<MethodInvoker<O>> cache, Object other) {
            IStructureModifier<T> structure = CachedReflectStructureProvider.<T>acquireStructure(other.getClass())
                    .withTarget(other);
            return (i1, i2) -> structure.invokeMethod(cache, 0, i1, i2);
        }
    }

    interface ProviderGenericInput<T> extends ProviderAdapter<T, Provider.ProviderGenericInput<T>, ProviderGenericInput<T>> {

        default <O> Provider.ProviderGenericInput<T> adaptRemote(AtomicReference<MethodInvoker<O>> cache, Object other) {
            IStructureModifier<T> structure = CachedReflectStructureProvider.<T>acquireStructure(other.getClass())
                    .withTarget(other);

            return array -> {
                // we need to cast to an Object to ensure the method interprets the varags correctly

                return structure.invokeMethod(cache, 0, (Object) array);
            };
        }
    }
}
