package me.alek.honey.netty.utility;

public interface IReflectMember<T> {

    Class<T> getType();

    String getName();
}
