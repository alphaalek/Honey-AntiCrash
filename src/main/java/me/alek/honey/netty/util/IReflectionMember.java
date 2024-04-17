package me.alek.honey.netty.util;

public interface IReflectionMember<T> {

    Class<T> getType();

    String getName();

    MemberType getMemberType();
}
