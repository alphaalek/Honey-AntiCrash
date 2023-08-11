package me.alek.honey.netty.utility.reflect;

import java.lang.reflect.Field;

public class FieldAccessor<T> implements ReflectToucher<T> {

    private final Field field;

    public FieldAccessor(Field field) {
        this.field = field;
        field.setAccessible(true);
    }

    public Field getField() {
        return field;
    }

    public T get(Object object) {
        try {
            return (T) field.get(object);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Reflection fejl");
        }
    }

    @Override
    public Class<T> getType() {
        return (Class<T>) field.getType();
    }

    @Override
    public String getName() {
        return field.getName();
    }

    public void set(Object object, T value) {
        try {
            field.set(object, value);
        } catch (Exception ex) {
            throw new RuntimeException("Reflection fejl");
        }
    }

    public boolean has(Object object) {
        return field.getDeclaringClass().isAssignableFrom(object.getClass());
    }
}
