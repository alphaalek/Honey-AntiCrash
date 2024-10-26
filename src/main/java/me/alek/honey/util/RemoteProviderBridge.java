package me.alek.honey.util;

import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.util.reflection.MethodInvoker;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;

public interface RemoteProviderBridge<T, P extends Provider<T, P, A>, A extends RemoteProviderBridge<T, P, A>> {

    default IStructureModifier<T> getStructure(Object other) {
        return ReflectStructure.Provider.<T>acquireStructure(other.getClass())
                .withTarget(other);
    }

    <O> P createProviderRemotely(AtomicReference<MethodInvoker<O>> cache,
                                 BiPredicate<AtomicInteger, MethodInvoker<?>> filter,
                                 Object other);

    class ProviderNoInput<T> implements RemoteProviderBridge<T, Provider.ProviderNoInput<T>, ProviderNoInput<T>> {

        public <O> Provider.ProviderNoInput<T> createProviderRemotely(AtomicReference<MethodInvoker<O>> cache,
                                                                      BiPredicate<AtomicInteger, MethodInvoker<?>> filter,
                                                                      Object other) {
            IStructureModifier<T> structure = getStructure(other);
            return () -> structure.invokeMethod(cache, filter);
        }
    }

    class ProviderOneInput<T, I1> implements RemoteProviderBridge<T, Provider.ProviderOneInput<T, I1>, ProviderOneInput<T, I1>> {

        public <O> Provider.ProviderOneInput<T, I1> createProviderRemotely(AtomicReference<MethodInvoker<O>> cache,
                                                                           BiPredicate<AtomicInteger, MethodInvoker<?>> filter,
                                                                           Object other) {
            IStructureModifier<T> structure = getStructure(other);
            return (i1) -> structure.invokeMethod(cache, filter, i1);
        }
    }

    class ProviderTwoInput<T, I1, I2> implements RemoteProviderBridge<T, Provider.ProviderTwoInput<T, I1, I2>, ProviderTwoInput<T, I1, I2>> {

        public <O> Provider.ProviderTwoInput<T, I1, I2> createProviderRemotely(AtomicReference<MethodInvoker<O>> cache,
                                                                               BiPredicate<AtomicInteger, MethodInvoker<?>> filter,
                                                                               Object other) {
            IStructureModifier<T> structure = getStructure(other);
            return (i1, i2) -> structure.invokeMethod(cache, filter, i1, i2);
        }
    }

    class ProviderGenericInput<T> implements RemoteProviderBridge<T, Provider.ProviderGenericInput<T>, ProviderGenericInput<T>> {

        public <O> Provider.ProviderGenericInput<T> createProviderRemotely(AtomicReference<MethodInvoker<O>> cache,
                                                                           BiPredicate<AtomicInteger, MethodInvoker<?>> filter,
                                                                           Object other) {
            IStructureModifier<T> structure = getStructure(other);
            return array -> {
                return structure.invokeMethod(cache, filter, array);
            };
        }
    }

    static <T> RemoteProviderBridge<T, Provider.ProviderNoInput<T>, RemoteProviderBridge.ProviderNoInput<T>> getWithNoInput() {
        return new RemoteProviderBridge.ProviderNoInput<T>();
    }

    static <T, I1> RemoteProviderBridge<T, Provider.ProviderOneInput<T, I1>, RemoteProviderBridge.ProviderOneInput<T, I1>> getWithOneInput() {
        return new RemoteProviderBridge.ProviderOneInput<T, I1>();
    }

    static <T, I1, I2> RemoteProviderBridge<T, Provider.ProviderTwoInput<T, I1, I2>, RemoteProviderBridge.ProviderTwoInput<T, I1, I2>> getWithTwoInput() {
        return new RemoteProviderBridge.ProviderTwoInput<T, I1, I2>();
    }

    static <T> RemoteProviderBridge<T, Provider.ProviderGenericInput<T>, RemoteProviderBridge.ProviderGenericInput<T>> getWithGenericInput() {
        return new RemoteProviderBridge.ProviderGenericInput<T>();
    }
}
