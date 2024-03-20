package me.alek.honey.netty.structure;

import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReflectStructureCache {

    public static Map<PacketTypeEnum, ReflectStructure<Object, Object>> PACKET_STRUCTURE_CACHE = new ConcurrentHashMap<>();
    public static Map<Class<?>, ReflectStructure<Object, Object>> WRAPPER_STRUCTURE_CACHE = new ConcurrentHashMap<>();

    public static synchronized ReflectStructure<Object, Object> acquireStructure(PacketTypeEnum packetType) {
        return PACKET_STRUCTURE_CACHE.computeIfAbsent(packetType, packetTypeEnum -> new ReflectStructure<>(packetType.getNmsClass()));
    }

    public static synchronized ReflectStructure<Object, Object> acquireStructure(Class<?> clazz) {
        return WRAPPER_STRUCTURE_CACHE.computeIfAbsent(clazz, wrapperClass -> new ReflectStructure<>(clazz));
    }

    public static synchronized ReflectStructure<Object, Object> acquirePacketStructureFromNMS(Class<?> nmsClass) {
        PacketTypeEnum packetType = PacketType.getPacketType(nmsClass);
        if (packetType == null) {
            throw new RuntimeException("Ingen packet type findes for " + nmsClass);
        }
        return acquireStructure(packetType);
    }
}
