package me.alek.honey.netty.utility;

public interface IReflectionMember<T> {

    Class<T> getType();

    String getName();
}
