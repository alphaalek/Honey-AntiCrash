package me.alek.honey.netty.packet.structure.converters;

public interface ApplicableConverter<T, C> extends JavaConverter<T, C> {

    ApplicableConverter<T, C> apply(T mapping);
}
