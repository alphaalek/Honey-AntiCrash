package me.alek.honey.util;

import me.alek.honey.HoneyClassLoader;
import me.alek.honey.util.reflection.MethodInvoker;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;

public class RemoteClasses {

    public static <O, T, P extends Provider<T, P>> P createWithException(AtomicReference<MethodInvoker<O>> cache, Provider<T, P> provider) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<?> clazz = HoneyClassLoader.get().loadClass(provider.getClass().getName());

        Object newProvider = clazz.getDeclaredConstructor().newInstance();
        return provider.adaptRemote(cache, newProvider);
    }

    public static <O, T, P extends Provider<T, P>> P create(AtomicReference<MethodInvoker<O>> cache, Provider<T, P> provider) {
        try {
            return createWithException(cache, provider);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to adapt remote class provider", ex);
        }
    }
}
