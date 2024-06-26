package me.alek.honey.util.reflection;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConstructorInvoker<T> {

    private final Constructor<T> constructor;

    private static <T> Constructor<T> getUnsafeConstructor(Class<T> clazz, Class<?>... params) {
        try {
            return clazz.getDeclaredConstructor(params);
        } catch (Exception ex) {
            throw new RuntimeException("Reflection error (constructor): name: " + clazz.getName() + ", params: " + Arrays.toString(params) + ", message: " + ex.getMessage());
        }
    }

    public ConstructorInvoker(Constructor<T> constructor) {
        this.constructor = constructor;
        this.constructor.setAccessible(true);
    }

    public ConstructorInvoker(Class<T> clazz, Class<?>... params) {
        this(getUnsafeConstructor(clazz, params));
    }

    public T invokeAndFixArrays(Object... params) {
        List<Object> newParams = new ArrayList<>();
        for (Object param : params) {
            if (param instanceof Object[]) {
                newParams.addAll(Arrays.asList((Object[]) param));
            } else {
                newParams.add(param);
            }
        }
        return invoke(newParams.toArray());
    }

    public T invoke(Object... params) {
        try {
            return constructor.newInstance(params);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Reflection error (constructor invoke): constructor: " + constructor + ", params: " + Arrays.toString(params) + ", message: " + ex.getMessage());
        }
    }

    public T invokeWithException(Object... parameters) throws Exception {
        return constructor.newInstance(parameters);
    }

    public Constructor<T> getConstructor() {
        return constructor;
    }
}