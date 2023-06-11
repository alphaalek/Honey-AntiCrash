package me.alek.honey.netty.api.packet;

import me.alek.honey.netty.utility.reflect.FieldAccessor;

import java.util.Collection;
import java.util.List;

public interface PacketStructure<T> {

    <R> PacketStructure<R> withType(Class<R> clazz);

    PacketModifier<T> withTarget(Object handle);

    Collection<FieldAccessor<T>> getAccessors();

    Collection<FieldAccessor<?>> getDefaultAccessors();
}
