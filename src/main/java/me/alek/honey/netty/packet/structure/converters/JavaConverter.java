package me.alek.honey.netty.packet.structure.converters;

public interface JavaConverter<T, C> {

    C convertDelegate(T delegate);

    T convertDeclaring(C object);

}
