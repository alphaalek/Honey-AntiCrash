package me.alek.honey.netty.utility;

import me.alek.honey.netty.structure.ReflectStructure;
import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class FieldAccessor<T> implements IReflectMember<T> {

    private final Field field;

    public FieldAccessor(Field field) {
        try {
            field.setAccessible(true);
        } catch (Exception ex) {

        }
        this.field = field;
    }

    public Field getField() {
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
    public String getName() {
        return field.getName();
    }

    public void set(Object object, T value) {
        try {
            field.set(object, value);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Reflection error (field set): " + field.getName() + " " + value.getClass().getSimpleName()
                    + " " + object.getClass().getSimpleName() + " " + ex.getMessage());
        }
    }

    public boolean has(Object object) {
        return field.getDeclaringClass().isAssignableFrom(object.getClass());
    }
}
