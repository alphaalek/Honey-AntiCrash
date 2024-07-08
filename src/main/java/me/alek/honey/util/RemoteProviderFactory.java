package me.alek.honey.util;

import me.alek.honey.HoneyClassLoader;
import me.alek.honey.util.reflection.MethodInvoker;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;

public class RemoteProviderFactory {

    public static <O, T, P extends Provider<T, P, A>, A extends ProviderAdapter<T, P, A>> P createWithException(AtomicReference<MethodInvoker<O>> cache,
                                                                                                                Class<? extends P> loadedClass,
                                                                                                                ProviderAdapter<T, P, A> adapter) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<?> clazz = HoneyClassLoader.get().loadClass(loadedClass.getName());
        Object newProvider = clazz.getDeclaredConstructor().newInstance();

        return adapter.adaptRemote(cache, newProvider);
    }

    public static <O, T, P extends Provider<T, P, A>, A extends ProviderAdapter<T, P, A>> P create(AtomicReference<MethodInvoker<O>> cache,
                                                                                                   Class<? extends P> loadedClass,
                                                                                                   ProviderAdapter<T, P, A> adapter) {
        try {
            return createWithException(cache, loadedClass, adapter);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to adapt remote class provider", ex);
        }
    }
}
