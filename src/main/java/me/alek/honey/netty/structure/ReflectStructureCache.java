package me.alek.honey.netty.structure;

import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.util.Array;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReflectStructureCache {

    public static Array<ReflectStructure<Object, Object>> PACKET_STRUCTURE_CACHE = new Array<>();
    public static Map<Class<?>, ReflectStructure<Object, Object>> WRAPPER_STRUCTURE_CACHE = new ConcurrentHashMap<>();

    public static synchronized ReflectStructure<Object, Object> acquireStructure(PacketType packetType) {
        return PACKET_STRUCTURE_CACHE.get(packetType.getHoneyID(), () -> new ReflectStructure<>(packetType.getNmsClass()));
    }

    public static synchronized <T> ReflectStructure<Object, T> acquireStructure(Class<?> clazz) {
        return (ReflectStructure<Object, T>) WRAPPER_STRUCTURE_CACHE.computeIfAbsent(clazz, wrapperClass -> new ReflectStructure<>(clazz));
    }
}
