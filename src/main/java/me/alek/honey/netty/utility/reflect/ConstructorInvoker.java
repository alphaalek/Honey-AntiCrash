package me.alek.honey.netty.utility.reflect;

import java.lang.reflect.Constructor;

public class ConstructorInvoker {

    private final Constructor constructor;

    public ConstructorInvoker(Constructor<?> constructor) {
        this.constructor = constructor;
    }

    public Object invoke(Object... parameters) {
        try {
            return constructor.newInstance(parameters);
        } catch (Exception ex) {
            throw new RuntimeException("Reflection fejl");
        }
    }
}