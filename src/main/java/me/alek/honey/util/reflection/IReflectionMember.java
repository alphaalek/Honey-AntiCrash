package me.alek.honey.util.reflection;

public interface IReflectionMember<T> {

    Object getHandle();

    Class<T> getType();

    Class<?> getDeclaringClass();

    String getName();

    int getModifiers();
}
