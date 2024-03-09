package me.alek.honey.netty.utility;

import java.lang.reflect.Method;

public class MethodInvoker<T> implements IReflectMember<T> {

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

    public Method getMethod() {
        return method;
    }

    @Override
    public Class<T> getType() {
        return (Class<T>) method.getReturnType();
    }

    @Override
    public String getName() {
        return method.getName();
    }
}
