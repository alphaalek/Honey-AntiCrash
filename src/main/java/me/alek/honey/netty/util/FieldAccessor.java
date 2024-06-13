package me.alek.honey.netty.util;

import java.lang.reflect.Field;

public class FieldAccessor<T> implements IReflectionMember<T> {

    private final Field field;

    public FieldAccessor(Field field) {
        try {
            field.setAccessible(true);
        } catch (Exception ex) {
        }
        this.field = field;
    }

    @Override
    public Field getHandle() {
        return field;
    }

    public T get(Object object) {
        try {
            return (T) field.get(object);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Reflection error (field get): " + field.getName() + " " + ex.getMessage());
        }
    }

    @Override
    public Class<T> getType() {
        return (Class<T>) field.getType();
    }

    @Override
    public Class<?> getDeclaringClass() {
        return field.getDeclaringClass();
    }

    @Override
    public String getName() {
        return field.getName();
    }

    @Override
    public int getModifiers() {
        return field.getModifiers();
    }

    public void set(Object object, T value) {
        try {
            field.set(object, value);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Reflection error (field set): name: " + field.getName() + " value: " + value.getClass().getSimpleName()
                    + " instance: " + object.getClass().getSimpleName() + " message: " + ex.getMessage());
        }
    }

    public boolean has(Object object) {
        return field.getDeclaringClass().isAssignableFrom(object.getClass());
    }
}
