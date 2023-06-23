package me.alek.honey.netty.utility.reflect;

import java.lang.reflect.Method;

public class MethodInvoker<T> implements ReflectToucher<T> {

    private final Method method;

    public MethodInvoker(Method method) {
        method.setAccessible(true);
        this.method = method;
    }

    public T invoke(Object instance, Object... parameters) {
        try {
            return invokeWithException(instance, parameters);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Reflection fejl");
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
