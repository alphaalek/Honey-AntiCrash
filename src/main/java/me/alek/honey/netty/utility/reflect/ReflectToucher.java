package me.alek.honey.netty.utility.reflect;

public interface ReflectToucher<T> {

    Class<T> getType();

    String getName();
}
