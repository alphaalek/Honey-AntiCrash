package me.alek.honey.netty.util;

public interface IReflectionMember<T> {

    Class<T> getType();

    Class<?> getDeclaringClass();

    String getName();

    MemberType getMemberType();
}
