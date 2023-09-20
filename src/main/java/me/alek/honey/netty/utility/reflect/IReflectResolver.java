package me.alek.honey.netty.utility.reflect;

public interface IReflectResolver<T> {

    Class<T> getType();

    String getName();
}
