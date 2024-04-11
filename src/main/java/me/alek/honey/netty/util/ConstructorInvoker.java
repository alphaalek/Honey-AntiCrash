package me.alek.honey.netty.util;

import java.lang.reflect.Constructor;
import java.util.Arrays;

public class ConstructorInvoker<T> {

    private final Constructor<T> constructor;

    private static <T> Constructor<T> getUnsafeConstructor(Class<T> clazz, Class<?>... params) {
        try {
            return clazz.getDeclaredConstructor(params);
        } catch (Exception ex) {
            throw new RuntimeException("Reflection error (constructor): " + clazz.getName() + " " + Arrays.toString(params) + " " + ex.getMessage());
        }
    }

    public ConstructorInvoker(Constructor<T> constructor) {
        this.constructor = constructor;
        this.constructor.setAccessible(true);
    }

    public ConstructorInvoker(Class<T> clazz, Class<?>... params) {
        this(getUnsafeConstructor(clazz, params));
    }

    public T invoke(Object... parameters) {
        try {
            return constructor.newInstance(parameters);
        } catch (Exception ex) {
            throw new RuntimeException("Reflection error (constructor invoke): " + constructor + " " + Arrays.toString(parameters) + " " + ex.getMessage());
        }
    }

    public T invokeWithException(Object... parameters) throws Exception {
        return constructor.newInstance(parameters);
    }

    public Constructor<T> getConstructor() {
        return constructor;
    }
}