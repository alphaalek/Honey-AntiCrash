package me.alek.honey.util;

import me.alek.honey.HoneyClassLoader;
import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.structure.CachedReflectStructureProvider;
import me.alek.honey.util.reflection.MethodInvoker;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;

public class RemoteProviderFactory {

    public static <O, T, P extends Provider<T, P, A>, A extends ProviderAdapter<T, P, A>> P createWithException(AtomicReference<MethodInvoker<O>> cache,
                                                                                                                Class<? extends P> loadedClass,
                                                                                                                BiPredicate<AtomicInteger, MethodInvoker<?>> filter,
                                                                                                                ProviderAdapter<T, P, A> adapter) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return adapter.adaptRemote(cache, filter, getNewProvider(loadedClass));
    }

    public static Object getNewProvider(Class<?> loadedClass) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        Class<?> clazz = HoneyClassLoader.get().loadClass(loadedClass.getName());
        return clazz.getDeclaredConstructor().newInstance();
    }

    public static Object getNewProviderWithException(Class<?> loadedClass) {
        try {
            return getNewProvider(loadedClass);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to create remote class provider", ex);
        }
    }

    public static <O, T, P extends Provider<T, P, A>, A extends ProviderAdapter<T, P, A>> P create(AtomicReference<MethodInvoker<O>> cache,
                                                                                                   Class<? extends P> loadedClass,
                                                                                                   BiPredicate<AtomicInteger, MethodInvoker<?>> filter,
                                                                                                   ProviderAdapter<T, P, A> adapter) {
        try {
            return createWithException(cache, loadedClass, filter, adapter);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to adapt remote class provider", ex);
        }
    }
}
