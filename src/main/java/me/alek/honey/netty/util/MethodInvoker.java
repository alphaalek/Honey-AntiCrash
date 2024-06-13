package me.alek.honey.netty.util;

import java.lang.reflect.Method;

public class MethodInvoker<T> implements IReflectionMember<T> {

    private final Method method;

    public MethodInvoker(Method method) {
        try {
            method.setAccessible(true);
        } catch (Exception ex) {

        }
        this.method = method;
    }

    public T invoke(Object instance, Object... parameters) {
        try {
            return invokeWithException(instance, parameters);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Reflection error (method invoke): " + method.getName() + " " + ex.getMessage());
        }
    }

    public T invokeWithException(Object instance, Object... parameters) throws Exception {
        return (T) method.invoke(instance, parameters);
    }

    @Override
    public Method getHandle() {
        return method;
    }

    @Override
    public Class<T> getType() {
        return (Class<T>) method.getReturnType();
    }

    @Override
    public Class<?> getDeclaringClass() {
        return method.getDeclaringClass();
    }

    @Override
    public String getName() {
        return method.getName();
    }

    @Override
    public int getModifiers() {
        return method.getModifiers();
    }
}
