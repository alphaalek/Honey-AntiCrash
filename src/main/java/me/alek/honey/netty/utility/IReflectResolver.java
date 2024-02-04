package me.alek.honey.netty.utility;

public interface IReflectResolver<T> {

    Class<T> getType();

    String getName();
}
