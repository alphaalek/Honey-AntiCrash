package me.alek.honey.util;

import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.structure.CachedReflectStructureProvider;
import me.alek.honey.util.reflection.MethodInvoker;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;

public interface ProviderAdapter<T, P extends Provider<T, P, A>, A extends ProviderAdapter<T, P, A>> {

    default IStructureModifier<T> getStructure(Object other) {
        return CachedReflectStructureProvider.<T>acquireStructure(other.getClass())
                .withTarget(other);
    }

    <O> P adaptRemote(AtomicReference<MethodInvoker<O>> cache,
                      BiPredicate<AtomicInteger, MethodInvoker<?>> filter,
                      Object other);

    interface ProviderNoInput<T> extends ProviderAdapter<T, Provider.ProviderNoInput<T>, ProviderNoInput<T>> {

        default <O> Provider.ProviderNoInput<T> adaptRemote(AtomicReference<MethodInvoker<O>> cache,
                                                            BiPredicate<AtomicInteger, MethodInvoker<?>> filter,
                                                            Object other) {
            IStructureModifier<T> structure = getStructure(other);
            return () -> structure.invokeMethod(cache, filter);
        }
    }

    interface ProviderOneInput<T, I1> extends ProviderAdapter<T, Provider.ProviderOneInput<T, I1>, ProviderOneInput<T, I1>> {

        default <O> Provider.ProviderOneInput<T, I1> adaptRemote(AtomicReference<MethodInvoker<O>> cache,
                                                                 BiPredicate<AtomicInteger, MethodInvoker<?>> filter,
                                                                 Object other) {
            IStructureModifier<T> structure = getStructure(other);
            return (i1) -> structure.invokeMethod(cache, filter, i1);
        }
    }

    interface ProviderTwoInput<T, I1, I2> extends ProviderAdapter<T, Provider.ProviderTwoInput<T, I1, I2>, ProviderTwoInput<T, I1, I2>> {

        default <O> Provider.ProviderTwoInput<T, I1, I2> adaptRemote(AtomicReference<MethodInvoker<O>> cache,
                                                                     BiPredicate<AtomicInteger, MethodInvoker<?>> filter,
                                                                     Object other) {
            IStructureModifier<T> structure = getStructure(other);
            return (i1, i2) -> structure.invokeMethod(cache, filter, i1, i2);
        }
    }

    interface ProviderGenericInput<T> extends ProviderAdapter<T, Provider.ProviderGenericInput<T>, ProviderGenericInput<T>> {

        default <O> Provider.ProviderGenericInput<T> adaptRemote(AtomicReference<MethodInvoker<O>> cache,
                                                                 BiPredicate<AtomicInteger, MethodInvoker<?>> filter,
                                                                 Object other) {
            IStructureModifier<T> structure = getStructure(other);
            return array -> {
                return structure.invokeMethod(cache, filter, array);
            };
        }
    }

    static <T> ProviderAdapter<T, Provider.ProviderNoInput<T>, ProviderAdapter.ProviderNoInput<T>> getWithNoInput() {

        return new ProviderAdapter.ProviderNoInput<T>() {
        };
    }

    static <T, I1> ProviderAdapter<T, Provider.ProviderOneInput<T, I1>, ProviderAdapter.ProviderOneInput<T, I1>> getWithOneInput() {

        return new ProviderAdapter.ProviderOneInput<T, I1>() {
        };
    }

    static <T, I1, I2> ProviderAdapter<T, Provider.ProviderTwoInput<T, I1, I2>, ProviderAdapter.ProviderTwoInput<T, I1, I2>> getWithTwoInput() {

        return new ProviderAdapter.ProviderTwoInput<T, I1, I2>() {
        };
    }

    static <T> ProviderAdapter<T, Provider.ProviderGenericInput<T>, ProviderAdapter.ProviderGenericInput<T>> getWithGenericInput() {

        return new ProviderAdapter.ProviderGenericInput<T>() {
        };
    }
}
