package me.alek.honey.netty.utility.reflect;

import java.lang.reflect.Method;

public class MethodInvoker {

    private final Method method;

    public MethodInvoker(Method method) {
        this.method = method;
    }

    public Object invoke(Object object, Object... parameters) {
        try {
            return method.invoke(object, parameters);
        } catch (Exception ex) {
            throw new RuntimeException("Reflection fejl");
        }
    }
}
