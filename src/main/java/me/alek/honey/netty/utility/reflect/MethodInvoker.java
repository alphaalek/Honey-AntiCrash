package me.alek.honey.netty.utility.reflect;

import java.lang.reflect.Method;

public class MethodInvoker {

    private final Method method;

    public MethodInvoker(Method method) {
        method.setAccessible(true);
        this.method = method;
    }

    public Object invoke(Object object, Object... parameters) {
        try {
            return method.invoke(object, parameters);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Reflection fejl");
        }
    }

    public Object invokeWithException(Object object, Object... parameters) throws Exception {
        return method.invoke(object, parameters);
    }

    public Method getMethod() {
        return method;
    }
}
