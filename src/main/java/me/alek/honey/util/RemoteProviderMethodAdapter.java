package me.alek.honey.util;

import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.structure.CachedReflectStructureProvider;
import me.alek.honey.util.reflection.MethodInvoker;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Function;

public class RemoteProviderMethodAdapter {

    @SuppressWarnings("unchecked")
    public static <T, C> T adapt(Object handle,
                   AtomicReference<MethodInvoker<Object>> cache,
                   BiPredicate<AtomicInteger, MethodInvoker<?>> filter,
                   Function<Provider.ProviderGenericInput<C>, T> adapter) {
        IStructureModifier<C> structureModifier = (IStructureModifier<C>) CachedReflectStructureProvider.acquireStructure(handle.getClass())
                .withTarget(handle);
        Provider.ProviderGenericInput<C> provider = array -> structureModifier.invokeMethod(cache, filter, array);

        return adapter.apply(provider);
    }
}