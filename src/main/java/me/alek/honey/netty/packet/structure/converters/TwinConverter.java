package me.alek.honey.netty.packet.structure.converters;

public interface TwinConverter<T, C> {

    C convertDelegate(T delegate);

    T convertDeclaring(C object);

}
